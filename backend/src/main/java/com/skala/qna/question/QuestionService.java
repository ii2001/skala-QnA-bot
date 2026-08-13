package com.skala.qna.question;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.skala.qna.organization.Enrollment;
import com.skala.qna.organization.EnrollmentRepository;
import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRepository;
import com.skala.qna.organization.UserRole;

@Service
@Transactional(readOnly = true)
public class QuestionService {

	private final QuestionRepository questions;
	private final UserRepository users;
	private final EnrollmentRepository enrollments;

	public QuestionService(QuestionRepository questions, UserRepository users, EnrollmentRepository enrollments) {
		this.questions = questions;
		this.users = users;
		this.enrollments = enrollments;
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
		return questions.save(new Question(author, enrollment.getCampus(), enrollment.getClassroom(), category, title, content));
	}

	public List<Question> questions(Long authorId) {
		return questions.findAllByAuthorIdOrderByCreatedAtDesc(authorId);
	}

	public Question question(Long id, Long authorId) {
		return questions.findByIdAndAuthorId(id, authorId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."));
	}
}
