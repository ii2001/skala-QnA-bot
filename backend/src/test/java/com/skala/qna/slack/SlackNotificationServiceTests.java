package com.skala.qna.slack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.skala.qna.organization.OrganizationService;
import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRole;
import com.skala.qna.question.Answer;
import com.skala.qna.question.AnswerRepository;
import com.skala.qna.question.AnswerVisibility;
import com.skala.qna.question.Question;
import com.skala.qna.question.QuestionRepository;
import com.skala.qna.question.QuestionService;

@SpringBootTest
@Import(SlackNotificationServiceTests.TestSlackConfiguration.class)
class SlackNotificationServiceTests {

	@Autowired
	private OrganizationService organization;

	@Autowired
	private QuestionService questions;

	@Autowired
	private QuestionRepository questionRepository;

	@Autowired
	private AnswerRepository answerRepository;

	@Autowired
	private SlackIntegrationService slack;

	@Autowired
	private RecordingSlackMessageSender sender;

	@BeforeEach
	void clearMessages() {
		sender.messages.clear();
		sender.fail = false;
	}

	@Test
	void notifiesEveryProfessorAssignedToTheQuestionClassroom() {
		Fixture fixture = fixture(2, true);

		questions.create(fixture.student().getId(), fixture.campusId(), fixture.classroomId(), "백엔드", "질문 제목",
				"질문 내용");

		assertThat(sender.messages).hasSize(2).extracting(Message::recipient)
				.containsExactlyInAnyOrderElementsOf(fixture.slackUserIds());
	}

	@Test
	void questionIsPersistedWhenSlackDeliveryFails() {
		Fixture fixture = fixture(1, true);
		sender.fail = true;

		Question question = questions.create(fixture.student().getId(), fixture.campusId(), fixture.classroomId(),
				"백엔드", "질문 제목", "질문 내용");

		assertThat(questionRepository.findById(question.getId())).isPresent();
		assertThat(sender.messages).hasSize(1);
	}

	@Test
	void answerIsPersistedWhenSlackDeliveryFails() {
		Fixture fixture = fixture(1, true);
		Question question = questions.create(fixture.student().getId(), fixture.campusId(), fixture.classroomId(),
				"백엔드", "질문 제목", "질문 내용");
		sender.messages.clear();
		sender.fail = true;

		Answer answer = questions.answer(question.getId(), fixture.professors().get(0).getId(), "답변 내용",
				AnswerVisibility.CLASS);

		assertThat(answerRepository.findById(answer.getId())).isPresent();
		assertThat(sender.messages).hasSize(1);
	}

	private Fixture fixture(int professorCount, boolean mapStudent) {
		String suffix = UUID.randomUUID().toString();
		var campus = organization.createCampus("알림 캠퍼스-" + suffix);
		var classroom = organization.createClassroom(campus.getId(), "알림 클래스-" + suffix);
		var student = organization.createUser("학생", "notification-student-" + suffix + "@example.com", UserRole.STUDENT);
		organization.enroll(student.getId(), campus.getId(), classroom.getId());
		List<User> professors = new ArrayList<>();
		List<String> slackUserIds = new ArrayList<>();
		for (int i = 0; i < professorCount; i++) {
			User professor = organization.createUser("교수", "notification-professor-" + i + "-" + suffix + "@example.com",
					UserRole.PROFESSOR);
			organization.assign(professor.getId(), classroom.getId());
			String slackUserId = "U" + UUID.randomUUID().toString().replace("-", "");
			slack.mapUser(professor.getId(), slackUserId);
			professors.add(professor);
			slackUserIds.add(slackUserId);
		}
		if (mapStudent) {
			slack.mapUser(student.getId(), "U" + UUID.randomUUID().toString().replace("-", ""));
		}
		return new Fixture(campus.getId(), classroom.getId(), student, professors, slackUserIds);
	}

	private record Fixture(Long campusId, Long classroomId, User student, List<User> professors,
			List<String> slackUserIds) {
	}

	private record Message(String recipient, String text) {
	}

	@TestConfiguration
	static class TestSlackConfiguration {

		@Bean
		@Primary
		RecordingSlackMessageSender recordingSlackMessageSender() {
			return new RecordingSlackMessageSender();
		}
	}

	static class RecordingSlackMessageSender implements SlackMessageSender {

		private final List<Message> messages = new ArrayList<>();
		private boolean fail;

		@Override
		public SlackSendResult send(String slackChannelId, String text) {
			messages.add(new Message(slackChannelId, text));
			if (fail) {
				throw new IllegalStateException("test failure");
			}
			return SlackSendResult.sent(slackChannelId, "test-timestamp");
		}
	}
}
