package com.skala.qna.slack;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.organization.OrganizationService;
import com.skala.qna.organization.UserRole;

@SpringBootTest
@Transactional
@Import(SlackIntegrationServiceTests.TestSlackConfiguration.class)
class SlackIntegrationServiceTests {

	@Autowired
	private OrganizationService organization;

	@Autowired
	private SlackIntegrationService slack;

	@Test
	void storesUserAndChannelMappingsAndUsesReplaceableSender() {
		String suffix = java.util.UUID.randomUUID().toString();
		var user = organization.createUser("Slack 학생", "slack-" + suffix + "@example.com", UserRole.STUDENT);

		SlackUserMapping userMapping = slack.mapUser(user.getId(), "U" + suffix.replace("-", ""));
		SlackChannelMapping channelMapping = slack.mapChannel("CLASS", 42L, "C" + suffix.replace("-", ""));
		SlackSendResult sendResult = slack.sendMessage(channelMapping.getSlackChannelId(), "테스트 메시지");

		assertThat(userMapping.getUser().getId()).isEqualTo(user.getId());
		assertThat(userMapping.getSlackUserId()).startsWith("U");
		assertThat(channelMapping.getScopeType()).isEqualTo("CLASS");
		assertThat(channelMapping.getScopeId()).isEqualTo(42L);
		assertThat(sendResult.sent()).isTrue();
		assertThat(sendResult.channelId()).isEqualTo(channelMapping.getSlackChannelId());
	}

	@TestConfiguration
	static class TestSlackConfiguration {

		@Bean
		@Primary
		SlackMessageSender fakeSlackMessageSender() {
			return (channelId, text) -> SlackSendResult.sent(channelId, "test-timestamp");
		}
	}
}
