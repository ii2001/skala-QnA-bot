package com.skala.qna.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.organization.OrganizationService;
import com.skala.qna.organization.UserRepository;
import com.skala.qna.organization.UserRole;
import com.skala.qna.slack.SlackUserMappingRepository;
import com.skala.qna.admin.StaffAccessService;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "SLACK_ALLOWED_TEAM_ID=T123")
class SlackLoginServiceTests {

	@Autowired
	private SlackLoginService slackLogin;

	@Autowired
	private OrganizationService organization;

	@Autowired
	private UserRepository users;

	@Autowired
	private SlackUserMappingRepository mappings;

	@Autowired
	private StaffAccessService staff;

	@Test
	void createsAndReusesStudentAndSlackIdentity() {
		OidcUser identity = identity("U123", "T123", "student@example.com", "학생");

		var first = slackLogin.login(identity);
		var second = slackLogin.login(identity);

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(second.getRole()).isEqualTo(UserRole.STUDENT);
		assertThat(users.count()).isEqualTo(1);
		assertThat(mappings.findBySlackUserId("U123").orElseThrow().getSlackTeamId()).isEqualTo("T123");
	}

	@Test
	void preservesExistingInternalRoleWhenLinkingByEmail() {
		var professor = organization.createUser("교수", "professor@example.com", UserRole.PROFESSOR);
		staff.create(professor.getEmail(), UserRole.PROFESSOR, "담당 교수", null);

		var linked = slackLogin.login(identity("U456", "T123", professor.getEmail(), professor.getName()));

		assertThat(linked.getId()).isEqualTo(professor.getId());
		assertThat(linked.getRole()).isEqualTo(UserRole.PROFESSOR);
	}

	@Test
	void rejectsUnverifiedOrUnapprovedSlackIdentity() {
		assertThatThrownBy(() -> slackLogin.login(identity("U789", "T999", "user@example.com", "사용자")))
				.hasMessageContaining("허용되지 않은 Slack Workspace");

		assertThatThrownBy(() -> slackLogin.login(identity("U789", "T123", "user@example.com", "사용자", false)))
				.hasMessageContaining("이메일을 검증");
	}

	private OidcUser identity(String slackUserId, String teamId, String email, String name) {
		return identity(slackUserId, teamId, email, name, true);
	}

	private OidcUser identity(String slackUserId, String teamId, String email, String name, boolean verified) {
		Instant now = Instant.now();
		OidcIdToken token = new OidcIdToken("test-token", now, now.plusSeconds(60), Map.of(
				"sub", slackUserId,
				"https://slack.com/user_id", slackUserId,
				"https://slack.com/team_id", teamId,
				"email", email,
				"email_verified", verified,
				"name", name));
		return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
	}
}
