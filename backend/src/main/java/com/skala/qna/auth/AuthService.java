package com.skala.qna.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResult login(String email, String rawPassword) {
		User user = users.findByEmail(email)
				.orElseThrow(this::invalidCredentials);
		if (!user.isActive() || user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw invalidCredentials();
		}
		return new LoginResult(jwtService.issue(user.getId(), user.getRole()), jwtService.expirationSeconds(), user);
	}

	public User user(Long userId) {
		return users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	private ResponseStatusException invalidCredentials() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
	}

	public record LoginResult(String accessToken, long expiresIn, User user) {
	}
}
