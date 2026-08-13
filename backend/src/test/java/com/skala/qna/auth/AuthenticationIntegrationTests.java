package com.skala.qna.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.skala.qna.organization.OrganizationService;
import com.skala.qna.organization.UserRepository;
import com.skala.qna.organization.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTests {

	private static final Pattern TOKEN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationService organization;

	@Autowired
	private UserRepository users;

	@BeforeEach
	void clearUsers() {
		users.deleteAll();
	}

	@Test
	void unauthenticatedRequestIsRejected() throws Exception {
		mockMvc.perform(get("/api/campuses")).andExpect(status().isUnauthorized());
	}

	@Test
	void loginTokenCanAccessProtectedApi() throws Exception {
		String token = login(UserRole.STUDENT);

		mockMvc.perform(get("/api/campuses").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void passwordsAreStoredAsOneWayHashes() {
		String email = createUser(UserRole.STUDENT);

		var user = users.findByEmail(email).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(user.getPasswordHash()).isNotEqualTo("password-123")
				.startsWith("$2");
	}

	@Test
	void invalidCredentialsAndTokensAreRejected() throws Exception {
		String email = createUser(UserRole.STUDENT);
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/campuses").header("Authorization", "Bearer invalid.token.value"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rolesAndOwnershipAreEnforced() throws Exception {
		String studentEmail = createUser(UserRole.STUDENT);
		String otherStudentEmail = createUser(UserRole.STUDENT);
		String professorToken = login(UserRole.PROFESSOR);
		String studentToken = login(studentEmail, "password-123");
		String adminToken = login(UserRole.ADMIN);

		mockMvc.perform(post("/api/campuses").header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"학생 캠퍼스\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + professorToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"사용자\",\"email\":\"new@example.com\",\"role\":\"STUDENT\",\"password\":\"password-123\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/campuses").header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"관리 캠퍼스\"}"))
				.andExpect(status().isCreated());

		Long otherStudentId = users.findByEmail(otherStudentEmail).orElseThrow().getId();
		mockMvc.perform(get("/api/students/{id}/enrollment", otherStudentId)
				.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isForbidden());
}

	private String login(UserRole role) throws Exception {
		return login(createUser(role), "password-123");
	}

	private String createUser(UserRole role) {
		return createUser(role, UUID.randomUUID().toString() + "@example.com");
	}

	private String createUser(UserRole role, String email) {
		organization.createUser(role.name(), email, role, "password-123");
		return email;
	}

	private String login(String email, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		Matcher matcher = TOKEN.matcher(body);
		if (!matcher.find()) {
			throw new AssertionError("Login response did not contain accessToken: " + body);
		}
		return matcher.group(1);
	}
}
