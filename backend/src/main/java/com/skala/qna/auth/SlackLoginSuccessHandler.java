package com.skala.qna.auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.skala.qna.organization.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SlackLoginSuccessHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

	private final SlackLoginService slackLogin;
	private final JwtService jwtService;
	private final String frontendOrigin;

	public SlackLoginSuccessHandler(SlackLoginService slackLogin, JwtService jwtService,
			@Value("${FRONTEND_ORIGIN:http://localhost:5173}") String frontendOrigin) {
		this.slackLogin = slackLogin;
		this.jwtService = jwtService;
		this.frontendOrigin = frontendOrigin.replaceAll("/+$", "");
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		try {
			if (!(authentication.getPrincipal() instanceof OidcUser identity)) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Slack OIDC identity가 아닙니다.");
			}
			User user = slackLogin.login(identity);
			String token = jwtService.issue(user.getId(), user.getRole());
			response.sendRedirect(redirect("access_token=" + encode(token)));
		} catch (ResponseStatusException exception) {
			redirectFailure(response, exception);
		}
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		redirectFailure(response, exception);
	}

	private void redirectFailure(HttpServletResponse response, Exception exception) throws IOException {
		String message = exception.getMessage() == null ? "Slack 로그인을 완료하지 못했습니다." : exception.getMessage();
		response.sendRedirect(redirect("auth_error=" + encode(message)));
	}

	private String redirect(String fragment) {
		return frontendOrigin + "/#" + fragment;
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
