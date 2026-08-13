package com.skala.qna.slack;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/slack")
@PreAuthorize("hasRole('ADMIN')")
public class SlackController {

	private final SlackIntegrationService slack;
	private final String testChannelId;

	public SlackController(SlackIntegrationService slack, @Value("${SLACK_TEST_CHANNEL_ID:}") String testChannelId) {
		this.slack = slack;
		this.testChannelId = testChannelId == null ? "" : testChannelId.trim();
	}

	@PostMapping("/test-message")
	public ResponseEntity<SlackSendResult> sendTestMessage(@Valid @RequestBody TestMessageRequest request) {
		if (testChannelId.isBlank()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(SlackSendResult.unavailable("SLACK_TEST_CHANNEL_ID가 설정되지 않았습니다."));
		}
		SlackSendResult result = slack.sendMessage(testChannelId, request.text());
		return ResponseEntity.status(result.sent() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(result);
	}

	@PostMapping("/user-mappings")
	public UserMappingResponse mapUser(@Valid @RequestBody UserMappingRequest request) {
		return UserMappingResponse.from(slack.mapUser(request.userId(), request.slackUserId()));
	}

	@GetMapping("/user-mappings/{userId}")
	public UserMappingResponse userMapping(@PathVariable Long userId) {
		return slack.userMapping(userId).map(UserMappingResponse::from)
				.orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,
						"Slack 사용자 매핑을 찾을 수 없습니다."));
	}

	@PostMapping("/channel-mappings")
	public ChannelMappingResponse mapChannel(@Valid @RequestBody ChannelMappingRequest request) {
		return ChannelMappingResponse.from(
				slack.mapChannel(request.scopeType(), request.scopeId(), request.slackChannelId()));
	}

	@GetMapping("/channel-mappings")
	public List<ChannelMappingResponse> channelMappings() {
		return slack.channelMappings().stream().map(ChannelMappingResponse::from).toList();
	}

	public record TestMessageRequest(@NotBlank String text) {
	}

	public record UserMappingRequest(@NotNull Long userId, @NotBlank String slackUserId) {
	}

	public record ChannelMappingRequest(@NotBlank String scopeType, Long scopeId,
			@NotBlank String slackChannelId) {
	}

	public record UserMappingResponse(Long id, Long userId, String slackUserId) {
		static UserMappingResponse from(SlackUserMapping mapping) {
			return new UserMappingResponse(mapping.getId(), mapping.getUser().getId(), mapping.getSlackUserId());
		}
	}

	public record ChannelMappingResponse(Long id, String scopeType, Long scopeId, String slackChannelId) {
		static ChannelMappingResponse from(SlackChannelMapping mapping) {
			return new ChannelMappingResponse(mapping.getId(), mapping.getScopeType(), mapping.getScopeId(),
					mapping.getSlackChannelId());
		}
	}
}
