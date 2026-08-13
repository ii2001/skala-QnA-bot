package com.skala.qna.slack;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SlackWebApiMessageSender implements SlackMessageSender {

	private static final URI CHAT_POST_MESSAGE = URI.create("https://slack.com/api/chat.postMessage");
	private static final URI OPEN_CONVERSATION = URI.create("https://slack.com/api/conversations.open");
	private static final Pattern JSON_FIELD = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"|(true|false|null))");

	private final String botToken;
	private final HttpClient httpClient;

	@Autowired
	public SlackWebApiMessageSender(@Value("${SLACK_BOT_TOKEN:}") String botToken) {
		this(botToken, HttpClient.newHttpClient());
	}

	SlackWebApiMessageSender(String botToken, HttpClient httpClient) {
		this.botToken = botToken == null ? "" : botToken.trim();
		this.httpClient = httpClient;
	}

	@Override
	public SlackSendResult send(String slackChannelId, String text) {
		validateMessageTarget(slackChannelId, text, "Slack 채널 ID");
		return postMessage(slackChannelId, text);
	}

	@Override
	public SlackSendResult sendDirectMessage(String slackUserId, String text) {
		validateMessageTarget(slackUserId, text, "Slack 사용자 ID");
		if (botToken.isBlank()) {
			return SlackSendResult.unavailable("SLACK_BOT_TOKEN이 설정되지 않았습니다.");
		}
		SlackSendResult conversation = post(OPEN_CONVERSATION,
				"{\"users\":\"" + escapeJson(slackUserId) + "\"}", null);
		if (!conversation.sent() || conversation.channelId() == null) {
			return conversation.sent()
					? SlackSendResult.unavailable("Slack DM 채널을 열었지만 채널 ID가 없습니다.")
					: conversation;
		}
		return postMessage(conversation.channelId(), text);
	}

	private SlackSendResult postMessage(String slackChannelId, String text) {
		if (botToken.isBlank()) {
			return SlackSendResult.unavailable("SLACK_BOT_TOKEN이 설정되지 않았습니다.");
		}
		return post(CHAT_POST_MESSAGE,
				"{\"channel\":\"" + escapeJson(slackChannelId) + "\",\"text\":\"" + escapeJson(text) + "\"}",
				slackChannelId);
	}

	private SlackSendResult post(URI endpoint, String body, String fallbackChannelId) {
		try {
			HttpRequest request = HttpRequest.newBuilder(endpoint)
				.header("Authorization", "Bearer " + botToken)
				.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return SlackSendResult.unavailable("Slack Web API HTTP 상태 코드가 " + response.statusCode() + "입니다.");
			}

			Optional<String> ok = jsonField(response.body(), "ok");
			if (ok.isEmpty()) {
				return SlackSendResult.unavailable("Slack Web API 응답을 해석하지 못했습니다.");
			}
			if (!"true".equals(ok.get())) {
				return SlackSendResult.unavailable("Slack Web API 오류: "
						+ jsonField(response.body(), "error").orElse("알 수 없는 오류"));
			}
			String channelId = fallbackChannelId == null ? jsonField(response.body(), "id").orElse(null)
					: fallbackChannelId;
			return SlackSendResult.sent(channelId, jsonField(response.body(), "ts").orElse(null));
		} catch (IOException exception) {
			return SlackSendResult.unavailable("Slack Web API 호출에 실패했습니다: " + exception.getMessage());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return SlackSendResult.unavailable("Slack Web API 호출이 중단되었습니다.");
		}
	}

	private void validateMessageTarget(String target, String text, String targetName) {
		if (target == null || target.isBlank() || text == null || text.isBlank()) {
			throw new IllegalArgumentException(targetName + "와 메시지는 필수입니다.");
		}
	}

	private Optional<String> jsonField(String json, String field) {
		Matcher matcher = JSON_FIELD.matcher(json == null ? "" : json);
		while (matcher.find()) {
			if (field.equals(matcher.group(1))) {
				return Optional.of(matcher.group(2) == null ? matcher.group(3) : unescapeJson(matcher.group(2)));
			}
		}
		return Optional.empty();
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
				.replace("\r", "\\r").replace("\t", "\\t");
	}

	private String unescapeJson(String value) {
		return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r")
				.replace("\\t", "\t").replace("\\\\", "\\");
	}
}
