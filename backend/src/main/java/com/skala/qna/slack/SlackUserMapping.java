package com.skala.qna.slack;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.skala.qna.organization.User;

@Entity
@Table(name = "slack_user_mappings")
public class SlackUserMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "slack_user_id", nullable = false, unique = true, length = 50)
	private String slackUserId;

	protected SlackUserMapping() {
	}

	public SlackUserMapping(User user, String slackUserId) {
		this.user = user;
		this.slackUserId = slackUserId;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getSlackUserId() {
		return slackUserId;
	}

	public void updateSlackUserId(String slackUserId) {
		this.slackUserId = slackUserId;
	}
}
