package com.skala.qna.slack;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "slack_channel_mappings")
public class SlackChannelMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "scope_type", nullable = false, length = 30)
	private String scopeType;

	@Column(name = "scope_id")
	private Long scopeId;

	@Column(name = "slack_channel_id", nullable = false, unique = true, length = 50)
	private String slackChannelId;

	protected SlackChannelMapping() {
	}

	public SlackChannelMapping(String scopeType, Long scopeId, String slackChannelId) {
		this.scopeType = scopeType;
		this.scopeId = scopeId;
		this.slackChannelId = slackChannelId;
	}

	public Long getId() {
		return id;
	}

	public String getScopeType() {
		return scopeType;
	}

	public Long getScopeId() {
		return scopeId;
	}

	public String getSlackChannelId() {
		return slackChannelId;
	}

	public void update(String scopeType, Long scopeId, String slackChannelId) {
		this.scopeType = scopeType;
		this.scopeId = scopeId;
		this.slackChannelId = slackChannelId;
	}
}
