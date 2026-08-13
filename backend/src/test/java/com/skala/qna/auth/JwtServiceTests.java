package com.skala.qna.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.skala.qna.organization.UserRole;

class JwtServiceTests {

	private static final String SECRET = "test-secret-for-jwt-tests-that-is-at-least-32-chars";

	@Test
	void issuedTokenContainsIdentityAndRejectsTampering() {
		JwtService jwt = new JwtService(SECRET, 3600);

		String token = jwt.issue(42L, UserRole.PROFESSOR);

		assertThat(jwt.parse(token).userId()).isEqualTo(42L);
		assertThat(jwt.parse(token).role()).isEqualTo(UserRole.PROFESSOR);
		assertThatThrownBy(() -> jwt.parse(token.substring(0, token.length() - 1) + "x"))
				.isInstanceOf(JwtService.InvalidTokenException.class);
	}

	@Test
	void expiredTokenIsRejected() {
		JwtService jwt = new JwtService(SECRET, -1);

		assertThatThrownBy(() -> jwt.parse(jwt.issue(42L, UserRole.STUDENT)))
				.isInstanceOf(JwtService.InvalidTokenException.class);
	}
}
