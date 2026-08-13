package com.skala.qna.slack;

public interface SlackMessageSender {

	SlackSendResult send(String slackChannelId, String text);
}
