package com.skala.qna.question;

import java.time.Instant;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.qna.auth.UserPrincipal;

@RestController
@RequestMapping("/api/professor/questions")
@PreAuthorize("hasRole('PROFESSOR')")
public class ProfessorQuestionController {

	private final QuestionService questions;

	public ProfessorQuestionController(QuestionService questions) {
		this.questions = questions;
	}

	@GetMapping
	public DashboardResponse questions(@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "campusId", required = false) Long campusId,
			@RequestParam(name = "classroomId", required = false) Long classroomId,
			@RequestParam(name = "category", required = false) String category) {
		List<Question> result = questions.professorQuestions(principal.userId(), status, campusId, classroomId, category);
		long unansweredCount = result.stream().filter(question -> question.getStatus() == QuestionStatus.OPEN).count();
		return new DashboardResponse(result.stream().map(ProfessorQuestionResponse::from).toList(), unansweredCount);
	}

	@GetMapping("/{id}")
	public ProfessorQuestionResponse question(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
		return ProfessorQuestionResponse.from(questions.professorQuestion(id, principal.userId()));
	}

	public record DashboardResponse(List<ProfessorQuestionResponse> questions, long unansweredCount) {
	}

	public record ProfessorQuestionResponse(Long id, Long authorId, Long campusId, String campusName, Long classroomId,
			String classroomName, String category, String title, String content, String status, String source,
			Instant createdAt) {
		static ProfessorQuestionResponse from(Question question) {
			return new ProfessorQuestionResponse(question.getId(), question.getAuthor().getId(), question.getCampus().getId(),
					question.getCampus().getName(), question.getClassroom().getId(), question.getClassroom().getName(),
					question.getCategory(), question.getTitle(), question.getContent(), question.getStatus().name(),
					question.getSource().name(), question.getCreatedAt());
		}
	}
}
