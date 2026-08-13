package com.skala.qna.question;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.skala.qna.organization.OrganizationService;
import com.skala.qna.organization.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
class ProfessorQuestionIntegrationTests {

	private static final Pattern TOKEN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationService organization;

	@Autowired
	private QuestionRepository questions;

	private String professorToken;
	private String studentToken;
	private Question assignedQuestion;
	private Question unrelatedQuestion;

	@BeforeEach
	void setUp() throws Exception {
		questions.deleteAll();
		String suffix = java.util.UUID.randomUUID().toString();
		var assignedCampus = organization.createCampus("담당 캠퍼스-" + suffix);
		var assignedClassroom = organization.createClassroom(assignedCampus.getId(), "담당 클래스-" + suffix);
		var unrelatedCampus = organization.createCampus("다른 캠퍼스-" + suffix);
		var unrelatedClassroom = organization.createClassroom(unrelatedCampus.getId(), "다른 클래스-" + suffix);
		var professor = organization.createUser("교수", "professor-" + suffix + "@example.com", UserRole.PROFESSOR,
				"password-123");
		var student = organization.createUser("학생", "student-" + suffix + "@example.com", UserRole.STUDENT,
				"password-123");
		organization.assign(professor.getId(), assignedClassroom.getId());
		assignedQuestion = questions.save(new Question(student, assignedCampus, assignedClassroom, "백엔드", "담당 질문",
				"담당 질문 내용"));
		questions.save(new Question(student, assignedCampus, assignedClassroom, "프론트엔드", "다른 담당 질문",
				"다른 담당 질문 내용"));
		unrelatedQuestion = questions.save(new Question(student, unrelatedCampus, unrelatedClassroom, "기타", "다른 질문",
				"다른 질문 내용"));
		professorToken = login(professor.getEmail());
		studentToken = login(student.getEmail());
	}

	@Test
	void professorSeesOnlyAssignedQuestionsAndFiltersThem() throws Exception {
		mockMvc.perform(get("/api/professor/questions").header("Authorization", "Bearer " + professorToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$.unansweredCount").value(2))
				.andExpect(jsonPath("$.questions", hasSize(2)))
				.andExpect(jsonPath("$.questions[0].campusName").value(org.hamcrest.Matchers.startsWith("담당 캠퍼스-")));

		mockMvc.perform(get("/api/professor/questions").param("category", "백엔드")
				.header("Authorization", "Bearer " + professorToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$.unansweredCount").value(1))
				.andExpect(jsonPath("$.questions", hasSize(1)))
				.andExpect(jsonPath("$.questions[0].title").value("담당 질문"));

		mockMvc.perform(get("/api/professor/questions").param("status", "OPEN")
				.param("campusId", assignedQuestion.getCampus().getId().toString())
				.param("classroomId", assignedQuestion.getClassroom().getId().toString())
				.header("Authorization", "Bearer " + professorToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$.questions", hasSize(2)));

		mockMvc.perform(get("/api/professor/questions").param("status", "CLOSED")
				.header("Authorization", "Bearer " + professorToken)).andExpect(status().isBadRequest());
	}

	@Test
	void detailAndRoleScopeAreEnforced() throws Exception {
		mockMvc.perform(get("/api/professor/questions/{id}", assignedQuestion.getId())
				.header("Authorization", "Bearer " + professorToken)).andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("담당 질문"));
		mockMvc.perform(get("/api/professor/questions/{id}", unrelatedQuestion.getId())
				.header("Authorization", "Bearer " + professorToken)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/professor/questions").header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isForbidden());
	}

	private String login(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		var matcher = TOKEN.matcher(body);
		if (!matcher.find()) {
			throw new AssertionError("Login response did not contain accessToken: " + body);
		}
		return matcher.group(1);
	}
}
