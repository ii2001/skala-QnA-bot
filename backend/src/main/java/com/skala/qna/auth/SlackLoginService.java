package com.skala.qna.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRepository;
import com.skala.qna.organization.UserRole;
import com.skala.qna.slack.SlackUserMapping;
import com.skala.qna.slack.SlackUserMappingRepository;

@Service
public class SlackLoginService {

	private final UserRepository users;
	private final SlackUserMappingRepository mappings;
	private final String allowedTeamId;

	public SlackLoginService(UserRepository users, SlackUserMappingRepository mappings,
			@Value("${SLACK_ALLOWED_TEAM_ID:}") String allowedTeamId) {
		this.users = users;
		this.mappings = mappings;
		this.allowedTeamId = allowedTeamId == null ? "" : allowedTeamId.trim();
	}

	@Transactional
	public User login(OidcUser identity) {
		String teamId = claim(identity, "https://slack.com/team_id", "team_id");
		String slackUserId = claim(identity, "https://slack.com/user_id", "sub");
		String email = identity.getEmail();
		if (identity.getClaimAsBoolean("email_verified") != Boolean.TRUE || email == null || email.isBlank()) {
			throw failure("Slack 이메일을 검증할 수 없습니다.", HttpStatus.UNAUTHORIZED);
		}
		if (!allowedTeamId.isBlank() && !allowedTeamId.equals(teamId)) {
			throw failure("허용되지 않은 Slack Workspace입니다.", HttpStatus.FORBIDDEN);
		}

		SlackUserMapping mapping = mappings.findBySlackUserId(slackUserId).orElse(null);
		User user = mapping == null ? users.findByEmail(email).orElseGet(() -> users.save(
				new User(displayName(identity, email), email, UserRole.STUDENT))) : mapping.getUser();
		if (mapping != null && !mapping.getUser().getEmail().equalsIgnoreCase(email)) {
			throw failure("Slack identity가 다른 사용자에 연결되어 있습니다.", HttpStatus.CONFLICT);
		}
		SlackUserMapping byUser = mappings.findByUserId(user.getId()).orElse(null);
		if (byUser != null && byUser != mapping && !byUser.getSlackUserId().equals(slackUserId)) {
			throw failure("내부 사용자에 다른 Slack identity가 연결되어 있습니다.", HttpStatus.CONFLICT);
		}
		if (mapping == null) {
			mappings.save(new SlackUserMapping(user, teamId, slackUserId));
		} else {
			mapping.updateIdentity(teamId, slackUserId);
		}
		return user;
	}

	private String claim(OidcUser identity, String primary, String fallback) {
		String value = identity.getClaimAsString(primary);
		if (value == null || value.isBlank()) value = identity.getClaimAsString(fallback);
		if (value == null || value.isBlank()) throw failure("Slack identity 정보가 부족합니다.", HttpStatus.UNAUTHORIZED);
		return value;
	}

	private String displayName(OidcUser identity, String email) {
		String name = identity.getFullName();
		return name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name;
	}

	private ResponseStatusException failure(String message, HttpStatus status) {
		return new ResponseStatusException(status, message);
	}
}
