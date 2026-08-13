package com.skala.qna.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class OrganizationServiceTests {

	@Autowired
	private OrganizationService organization;

	@Test
	void studentEnrollmentMustUseAClassroomInTheSelectedCampus() {
		Campus seoul = organization.createCampus("서울");
		Campus busan = organization.createCampus("부산");
		Classroom classroom = organization.createClassroom(seoul.getId(), "1반");
		User student = organization.createUser("학생", "student@example.com", UserRole.STUDENT);

		assertThatThrownBy(() -> organization.enroll(student.getId(), busan.getId(), classroom.getId()))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("클래스가 선택한 캠퍼스에 속하지 않습니다");

		Enrollment enrollment = organization.enroll(student.getId(), seoul.getId(), classroom.getId());
		assertThat(enrollment.getCampus().getId()).isEqualTo(seoul.getId());
		assertThat(enrollment.getClassroom().getId()).isEqualTo(classroom.getId());
	}

	@Test
	void professorCanBeAssignedToMultipleCampusesAndClassrooms() {
		Campus seoul = organization.createCampus("서울");
		Campus busan = organization.createCampus("부산");
		Classroom first = organization.createClassroom(seoul.getId(), "1반");
		Classroom second = organization.createClassroom(busan.getId(), "2반");
		User professor = organization.createUser("교수", "professor@example.com", UserRole.PROFESSOR);

		organization.assign(professor.getId(), first.getId());
		organization.assign(professor.getId(), second.getId());

		assertThat(organization.assignments(professor.getId()))
				.extracting(assignment -> assignment.getClassroom().getId())
				.containsExactlyInAnyOrder(first.getId(), second.getId());
	}

	@Test
	void classroomCanHaveMultipleProfessorsButNotDuplicateAssignments() {
		Campus campus = organization.createCampus("서울");
		Classroom classroom = organization.createClassroom(campus.getId(), "1반");
		User first = organization.createUser("교수1", "professor1@example.com", UserRole.PROFESSOR);
		User second = organization.createUser("교수2", "professor2@example.com", UserRole.PROFESSOR);

		organization.assign(first.getId(), classroom.getId());
		organization.assign(second.getId(), classroom.getId());

		assertThat(organization.assignments(first.getId())).hasSize(1);
		assertThat(organization.assignments(second.getId())).hasSize(1);
		assertThatThrownBy(() -> organization.assign(first.getId(), classroom.getId()))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("이미 담당 중인 클래스입니다");
	}
}
