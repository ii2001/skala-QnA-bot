package com.skala.qna.question;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.skala.qna.organization.Enrollment;
import com.skala.qna.organization.EnrollmentRepository;
import com.skala.qna.organization.ProfessorAssignmentRepository;
import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRepository;
import com.skala.qna.organization.UserRole;

@Service
@Transactional(readOnly = true)
public class QuestionService {

	private final QuestionRepository questions;
	private final UserRepository users;
	private final EnrollmentRepository enrollments;
	private final ProfessorAssignmentRepository assignments;

	public QuestionService(QuestionRepository questions, UserRepository users, EnrollmentRepository enrollments,
			ProfessorAssignmentRepository assignments) {
		this.questions = questions;
		this.users = users;
		this.enrollments = enrollments;
		this.assignments = assignments;
	}

	@Transactional
	public Question create(Long authorId, Long campusId, Long classroomId, String category, String title, String content) {
		User author = users.findById(authorId)
				.filter(user -> user.getRole() == UserRole.STUDENT)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "학생만 질문을 등록할 수 있습니다."));
		Enrollment enrollment = enrollments.findByStudentId(authorId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 등록 정보를 찾을 수 없습니다."));
		if (!enrollment.getCampus().getId().equals(campusId)
				|| !enrollment.getClassroom().getId().equals(classroomId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록된 캠퍼스와 클래스만 선택할 수 있습니다.");
		}
		return questions.save(new Question(author, enrollment.getCampus(), enrollment.getClassroom(), category.trim(), title, content));
	}

	public List<Question> questions(Long authorId) {
		return questions.findAllByAuthorIdOrderByCreatedAtDesc(authorId);
	}

	public Question question(Long id, Long authorId) {
		return questions.findByIdAndAuthorId(id, authorId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."));
	}

	public List<Question> professorQuestions(Long professorId, String status, Long campusId, Long classroomId,
			String category) {
		QuestionStatus requestedStatus = parseStatus(status);
		Set<Long> classroomIds = assignedClassroomIds(professorId);
		if (classroomIds.isEmpty()) {
			return List.of();
		}
		String requestedCategory = category == null || category.isBlank() ? null : category.trim();
		return questions.findAllByClassroomIdInOrderByCreatedAtDesc(classroomIds).stream()
				.filter(question -> requestedStatus == null || question.getStatus() == requestedStatus)
				.filter(question -> campusId == null || question.getCampus().getId().equals(campusId))
				.filter(question -> classroomId == null || question.getClassroom().getId().equals(classroomId))
				.filter(question -> requestedCategory == null || question.getCategory().equals(requestedCategory))
				.toList();
	}

	public Question professorQuestion(Long id, Long professorId) {
		Set<Long> classroomIds = assignedClassroomIds(professorId);
		if (classroomIds.isEmpty()) {
			throw questionNotFound();
		}
		return questions.findByIdAndClassroomIdIn(id, classroomIds).orElseThrow(this::questionNotFound);
	}

	private Set<Long> assignedClassroomIds(Long professorId) {
		return assignments.findAllByProfessorId(professorId).stream()
				.map(assignment -> assignment.getClassroom().getId())
				.collect(Collectors.toSet());
	}

	private QuestionStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return QuestionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 질문 상태입니다.");
		}
	}

	private ResponseStatusException questionNotFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다.");
	}
}
