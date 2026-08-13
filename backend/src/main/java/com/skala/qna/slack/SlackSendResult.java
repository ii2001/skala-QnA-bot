package com.skala.qna.slack;

public record SlackSendResult(boolean sent, String channelId, String timestamp, String error) {

	public static SlackSendResult sent(String channelId, String timestamp) {
		return new SlackSendResult(true, channelId, timestamp, null);
	}

	public static SlackSendResult unavailable(String error) {
		return new SlackSendResult(false, null, null, error);
	}
}
