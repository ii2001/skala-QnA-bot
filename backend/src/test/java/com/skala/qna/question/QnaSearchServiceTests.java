package com.skala.qna.question;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.auth.UserPrincipal;
import com.skala.qna.organization.OrganizationService;
import com.skala.qna.organization.UserRole;

@SpringBootTest
@Transactional
class QnaSearchServiceTests {

	@Autowired
	private QnaSearchService search;

	@Autowired
	private OrganizationService organization;

	@Autowired
	private QuestionRepository questions;

	@Autowired
	private AnswerRepository answers;

	@Test
	void appliesVisibilityForStudentAndAssignedProfessor() {
		var campus = organization.createCampus("검색 캠퍼스");
		var classroom = organization.createClassroom(campus.getId(), "검색 클래스");
		var student = organization.createUser("학생1", "search-student-1@example.com", UserRole.STUDENT);
		var otherStudent = organization.createUser("학생2", "search-student-2@example.com", UserRole.STUDENT);
		var professor = organization.createUser("교수", "search-professor@example.com", UserRole.PROFESSOR);
		organization.enroll(student.getId(), campus.getId(), classroom.getId());
		organization.enroll(otherStudent.getId(), campus.getId(), classroom.getId());
		organization.assign(professor.getId(), classroom.getId());

		Question privateQuestion = answered(student, "개인 질문", AnswerVisibility.PRIVATE);
		Question otherPrivateQuestion = answered(otherStudent, "다른 개인 질문", AnswerVisibility.PRIVATE);
		Question globalQuestion = answered(otherStudent, "전체 질문", AnswerVisibility.GLOBAL);

		var studentResults = search.search(new UserPrincipal(student.getId(), UserRole.STUDENT), "질문", null, null, null);
		var professorResults = search.search(new UserPrincipal(professor.getId(), UserRole.PROFESSOR), "질문", null, null, null);

		assertThat(studentResults).extracting(QnaSearchService.Result::id)
				.containsExactlyInAnyOrder(privateQuestion.getId(), globalQuestion.getId());
		assertThat(studentResults).extracting(QnaSearchService.Result::id).doesNotContain(otherPrivateQuestion.getId());
		assertThat(professorResults).extracting(QnaSearchService.Result::id)
				.containsExactlyInAnyOrder(privateQuestion.getId(), otherPrivateQuestion.getId(), globalQuestion.getId());
	}

	private Question answered(com.skala.qna.organization.User author, String title, AnswerVisibility visibility) {
		var campus = organization.campuses().get(0);
		var classroom = organization.classrooms(campus.getId()).get(0);
		Question question = questions.save(new Question(author, campus, classroom, "일반", title, title + " 내용"));
		answers.save(new Answer(question, organization.users().stream().filter(user -> user.getRole() == UserRole.PROFESSOR).findFirst().orElseThrow(), "답변", visibility));
		question.markAnswered();
		return question;
	}
}
