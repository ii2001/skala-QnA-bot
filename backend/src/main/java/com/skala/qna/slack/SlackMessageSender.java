package com.skala.qna.slack;

public interface SlackMessageSender {

	SlackSendResult send(String slackChannelId, String text);

	default SlackSendResult sendDirectMessage(String slackUserId, String text) {
		return send(slackUserId, text);
	}
}
