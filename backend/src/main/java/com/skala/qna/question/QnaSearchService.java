package com.skala.qna.question;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.auth.UserPrincipal;
import com.skala.qna.organization.Enrollment;
import com.skala.qna.organization.EnrollmentRepository;
import com.skala.qna.organization.ProfessorAssignmentRepository;
import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRepository;
import com.skala.qna.organization.UserRole;

@Service
@Transactional(readOnly = true)
public class QnaSearchService {

	private final AnswerRepository answers;
	private final UserRepository users;
	private final EnrollmentRepository enrollments;
	private final ProfessorAssignmentRepository assignments;

	public QnaSearchService(AnswerRepository answers, UserRepository users, EnrollmentRepository enrollments,
			ProfessorAssignmentRepository assignments) {
		this.answers = answers;
		this.users = users;
		this.enrollments = enrollments;
		this.assignments = assignments;
	}

	public List<Result> search(UserPrincipal principal, String query, String category, Long campusId, Long classroomId) {
		User user = users.findById(principal.userId()).orElseThrow();
		Enrollment enrollment = enrollments.findByStudentId(user.getId()).orElse(null);
		Set<Long> assignedClassrooms = assignments.findAllByProfessorId(user.getId()).stream()
				.map(assignment -> assignment.getClassroom().getId()).collect(Collectors.toSet());
		String normalizedQuery = query == null ? "" : query.trim();
		return answers.search(normalizedQuery).stream()
				.filter(answer -> canView(answer, user, enrollment, assignedClassrooms))
				.filter(answer -> category == null || category.isBlank() || answer.getQuestion().getCategory().equals(category.trim()))
				.filter(answer -> campusId == null || answer.getQuestion().getCampus().getId().equals(campusId))
				.filter(answer -> classroomId == null || answer.getQuestion().getClassroom().getId().equals(classroomId))
				.map(Result::from)
				.toList();
	}

	private boolean canView(Answer answer, User user, Enrollment enrollment, Set<Long> assignedClassrooms) {
		if (user.getRole() == UserRole.ADMIN || assignedClassrooms.contains(answer.getQuestion().getClassroom().getId())) return true;
		return switch (answer.getVisibility()) {
		case PRIVATE -> answer.getQuestion().getAuthor().getId().equals(user.getId());
		case CLASS -> enrollment != null && enrollment.getClassroom().getId().equals(answer.getQuestion().getClassroom().getId());
		case CAMPUS -> enrollment != null && enrollment.getCampus().getId().equals(answer.getQuestion().getCampus().getId());
		case GLOBAL -> true;
		};
	}

	public record Result(Long id, String category, String title, String questionContent, String answerContent,
			String visibility, Long campusId, String campusName, Long classroomId, String classroomName, Instant createdAt) {
		static Result from(Answer answer) {
			Question question = answer.getQuestion();
			return new Result(question.getId(), question.getCategory(), question.getTitle(), question.getContent(),
					answer.getContent(), answer.getVisibility().name(), question.getCampus().getId(), question.getCampus().getName(),
					question.getClassroom().getId(), question.getClassroom().getName(), answer.getCreatedAt());
		}
	}
}
