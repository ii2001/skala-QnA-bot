package com.skala.qna.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@ConditionalOnExpression("'${SLACK_CLIENT_ID:}' != '' && '${SLACK_CLIENT_SECRET:}' != '' && '${SLACK_REDIRECT_URI:}' != ''")
public class SlackOAuthClientConfiguration {

	@Bean
	ClientRegistrationRepository slackClientRegistration(
			@Value("${SLACK_CLIENT_ID}") String clientId,
			@Value("${SLACK_CLIENT_SECRET}") String clientSecret,
			@Value("${SLACK_REDIRECT_URI}") String redirectUri) {
		ClientRegistration registration = ClientRegistration.withRegistrationId("slack")
				.clientId(clientId)
				.clientSecret(clientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(redirectUri)
				.scope("openid", "email", "profile")
				.authorizationUri("https://slack.com/openid/connect/authorize")
				.tokenUri("https://slack.com/api/openid.connect.token")
				.jwkSetUri("https://slack.com/openid/connect/keys")
				.userInfoUri("https://slack.com/api/openid.connect.userInfo")
				.userNameAttributeName("sub")
				.clientName("Slack")
				.build();
		return new InMemoryClientRegistrationRepository(registration);
	}
}
