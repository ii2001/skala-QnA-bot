package com.skala.qna.question;

import static org.assertj.core.api.Assertions.assertThat;
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
class AnswerIntegrationTests {

	private static final Pattern TOKEN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationService organization;

	@Autowired
	private QuestionRepository questions;

	@Autowired
	private AnswerRepository answers;

	private Question question;
	private String professorToken;
	private String studentToken;
	private String unrelatedProfessorEmail;

	@BeforeEach
	void setUp() throws Exception {
		answers.deleteAll();
		questions.deleteAll();
		String suffix = java.util.UUID.randomUUID().toString();
		var campus = organization.createCampus("답변 캠퍼스-" + suffix);
		var classroom = organization.createClassroom(campus.getId(), "답변 클래스-" + suffix);
		var unrelatedCampus = organization.createCampus("무관 캠퍼스-" + suffix);
		var unrelatedClassroom = organization.createClassroom(unrelatedCampus.getId(), "무관 클래스-" + suffix);
		var professor = organization.createUser("교수", "professor-" + suffix + "@example.com", UserRole.PROFESSOR,
				"password-123");
		var unrelatedProfessor = organization.createUser("다른 교수", "other-professor-" + suffix + "@example.com",
				UserRole.PROFESSOR, "password-123");
		unrelatedProfessorEmail = unrelatedProfessor.getEmail();
		var student = organization.createUser("학생", "student-" + suffix + "@example.com", UserRole.STUDENT,
				"password-123");
		organization.assign(professor.getId(), classroom.getId());
		organization.assign(unrelatedProfessor.getId(), unrelatedClassroom.getId());
		question = questions.save(new Question(student, campus, classroom, "백엔드", "답변할 질문", "질문 내용"));
		professorToken = login(professor.getEmail());
		studentToken = login(student.getEmail());
	}

	@Test
	void authorizedProfessorCreatesAnswerAndMarksQuestionAnswered() throws Exception {
		mockMvc.perform(post("/api/professor/questions/{id}/answers", question.getId())
				.header("Authorization", "Bearer " + professorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(answerJson("CLASS")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.questionId").value(question.getId()))
				.andExpect(jsonPath("$.professorId").exists())
				.andExpect(jsonPath("$.content").value("답변 내용"))
				.andExpect(jsonPath("$.visibility").value("CLASS"));

		assertThat(answers.findByQuestionId(question.getId())).isPresent();
		assertThat(questions.findById(question.getId()).orElseThrow().getStatus()).isEqualTo(QuestionStatus.ANSWERED);
	}

	@Test
	void eachVisibilityIsPersistedAndDuplicateAnswersAreRejected() throws Exception {
		for (String visibility : new String[] { "PRIVATE", "CLASS", "CAMPUS", "GLOBAL" }) {
			Question next = questions.save(new Question(question.getAuthor(), question.getCampus(), question.getClassroom(),
					"기타", visibility + " 질문", "내용"));
			mockMvc.perform(post("/api/professor/questions/{id}/answers", next.getId())
					.header("Authorization", "Bearer " + professorToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(answerJson(visibility)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.visibility").value(visibility));
		}

		mockMvc.perform(post("/api/professor/questions/{id}/answers", question.getId())
				.header("Authorization", "Bearer " + professorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(answerJson("PRIVATE")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/professor/questions/{id}/answers", question.getId())
				.header("Authorization", "Bearer " + professorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(answerJson("GLOBAL")))
				.andExpect(status().isConflict());
		assertThat(answers.count()).isEqualTo(5);
	}

	@Test
	void onlyProfessorAssignedToQuestionClassroomCanAnswer() throws Exception {
		String unrelatedProfessorToken = login(unrelatedProfessorEmail);
		mockMvc.perform(post("/api/professor/questions/{id}/answers", question.getId())
				.header("Authorization", "Bearer " + unrelatedProfessorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(answerJson("PRIVATE")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/professor/questions/{id}/answers", question.getId())
				.header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(answerJson("PRIVATE")))
				.andExpect(status().isForbidden());
	}

	private String answerJson(String visibility) {
		return "{\"content\":\"답변 내용\",\"visibility\":\"" + visibility + "\"}";
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
