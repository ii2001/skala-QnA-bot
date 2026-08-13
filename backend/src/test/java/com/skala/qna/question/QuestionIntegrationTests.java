package com.skala.qna.question;

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
class QuestionIntegrationTests {

	private static final Pattern TOKEN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationService organization;

	@Autowired
	private QuestionRepository questions;

	private Long studentId;
	private Long campusId;
	private Long classroomId;
	private String studentToken;

	@BeforeEach
	void setUp() throws Exception {
		questions.deleteAll();
		String suffix = java.util.UUID.randomUUID().toString();
		var campus = organization.createCampus("캠퍼스-" + suffix);
		var classroom = organization.createClassroom(campus.getId(), "클래스-" + suffix);
		var student = organization.createUser("학생", "student-" + suffix + "@example.com", UserRole.STUDENT,
				"password-123");
		organization.enroll(student.getId(), campus.getId(), classroom.getId());
		studentId = student.getId();
		campusId = campus.getId();
		classroomId = classroom.getId();
		studentToken = login(student.getEmail());
	}

	@Test
	void studentCreatesAndReadsOwnQuestion() throws Exception {
		String created = mockMvc.perform(post("/api/questions").header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON).content(questionJson(campusId, classroomId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.authorId").value(studentId))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.source").value("WEB"))
				.andReturn().getResponse().getContentAsString();
		Long questionId = Long.valueOf(created.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));

		mockMvc.perform(get("/api/questions").header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(questionId));
		mockMvc.perform(get("/api/questions/{id}", questionId).header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Spring 질문"));
	}

	@Test
	void categoryIsTrimmedBeforeStorage() throws Exception {
		mockMvc.perform(post("/api/questions").header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(questionJson(campusId, classroomId).replace("\\\"백엔드\\\"", "\\\" 백엔드 \\\"")))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.category").value("백엔드"));
	}

	@Test
	void invalidEnrollmentAndValidationAreRejected() throws Exception {
		var otherCampus = organization.createCampus("다른 캠퍼스-" + java.util.UUID.randomUUID());
		var otherClassroom = organization.createClassroom(otherCampus.getId(), "다른 클래스");

		mockMvc.perform(post("/api/questions").header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON).content(questionJson(otherCampus.getId(), otherClassroom.getId())))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/questions").header("Authorization", "Bearer " + studentToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"campusId\":" + campusId + ",\"classroomId\":" + classroomId
						+ ",\"category\":\"\",\"title\":\"\",\"content\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("입력값을 확인해 주세요."))
				.andExpect(jsonPath("$.errors.category").exists());
	}

	@Test
	void authenticationRoleAndOwnershipAreEnforced() throws Exception {
		mockMvc.perform(get("/api/questions")).andExpect(status().isUnauthorized());
		String professorEmail = "professor-" + java.util.UUID.randomUUID() + "@example.com";
		organization.createUser("교수", professorEmail, UserRole.PROFESSOR, "password-123");
		mockMvc.perform(get("/api/questions").header("Authorization", "Bearer " + login(professorEmail)))
				.andExpect(status().isForbidden());

		var question = questions.save(new Question(
				organization.createUser("다른 학생", "other-" + java.util.UUID.randomUUID() + "@example.com", UserRole.STUDENT),
				organization.campuses().stream().filter(campus -> campus.getId().equals(campusId)).findFirst().orElseThrow(),
				organization.classrooms(campusId).stream().filter(room -> room.getId().equals(classroomId)).findFirst().orElseThrow(),
				"기타", "비공개 질문", "다른 학생의 질문"));
		mockMvc.perform(get("/api/questions/{id}", question.getId()).header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isNotFound());
	}

	private String questionJson(Long selectedCampusId, Long selectedClassroomId) {
		return "{\"authorId\":999999,\"campusId\":" + selectedCampusId + ",\"classroomId\":" + selectedClassroomId
				+ ",\"category\":\"백엔드\",\"title\":\"Spring 질문\",\"content\":\"내용입니다.\"}";
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
