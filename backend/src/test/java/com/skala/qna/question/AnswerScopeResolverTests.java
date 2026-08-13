package com.skala.qna.question;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.organization.OrganizationService;
import com.skala.qna.organization.ClassroomRepository;
import com.skala.qna.organization.UserRole;

@SpringBootTest
@Transactional
class AnswerScopeResolverTests {

	@Autowired
	private OrganizationService organization;

	@Autowired
	private AnswerScopeResolver scopes;

	@Autowired
	private ClassroomRepository classrooms;

	@Test
	void resolvesPrivateClassCampusAndGlobalScopesWithoutDelivery() {
		String suffix = java.util.UUID.randomUUID().toString();
		var firstCampus = organization.createCampus("첫 캠퍼스-" + suffix);
		var secondCampus = organization.createCampus("둘째 캠퍼스-" + suffix);
		var firstClass = organization.createClassroom(firstCampus.getId(), "첫 클래스-" + suffix);
		var secondClass = organization.createClassroom(firstCampus.getId(), "둘째 클래스-" + suffix);
		var thirdClass = organization.createClassroom(secondCampus.getId(), "셋째 클래스-" + suffix);
		var student = organization.createUser("학생", "student-" + suffix + "@example.com", UserRole.STUDENT);
		Question question = new Question(student, firstCampus, firstClass, "기타", "질문", "내용");

		assertThat(scopes.resolve(question, AnswerVisibility.PRIVATE).studentIds()).containsExactly(student.getId());
		assertThat(scopes.resolve(question, AnswerVisibility.PRIVATE).classroomIds()).isEmpty();
		assertThat(scopes.resolve(question, AnswerVisibility.CLASS).classroomIds()).containsExactly(firstClass.getId());
		assertThat(scopes.resolve(question, AnswerVisibility.CAMPUS).classroomIds())
				.containsExactlyInAnyOrder(firstClass.getId(), secondClass.getId());
		assertThat(scopes.resolve(question, AnswerVisibility.GLOBAL).classroomIds())
				.containsExactlyInAnyOrderElementsOf(classrooms.findAll().stream().map(classroom -> classroom.getId()).toList());
	}
}
