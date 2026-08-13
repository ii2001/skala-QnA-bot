package com.skala.qna.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.qna.organization.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		AuthService.LoginResult result = authService.login(request.email(), request.password());
		return LoginResponse.from(result);
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
		static LoginResponse from(AuthService.LoginResult result) {
			return new LoginResponse(result.accessToken(), "Bearer", result.expiresIn(), UserResponse.from(result.user()));
		}
	}

	public record UserResponse(Long id, String name, String email, String role) {
		static UserResponse from(User user) {
			return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
		}
	}
}
