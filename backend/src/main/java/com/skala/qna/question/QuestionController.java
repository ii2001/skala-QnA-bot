package com.skala.qna.question;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skala.qna.auth.UserPrincipal;

@RestController
@RequestMapping("/api/questions")
@PreAuthorize("hasRole('STUDENT')")
public class QuestionController {

	private final QuestionService questionService;

	public QuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QuestionResponse create(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateQuestionRequest request) {
		return QuestionResponse.from(questionService.create(principal.userId(), request.campusId(), request.classroomId(),
				request.category(), request.title(), request.content()));
	}

	@GetMapping
	public List<QuestionResponse> questions(@AuthenticationPrincipal UserPrincipal principal) {
		return questionService.questions(principal.userId()).stream().map(QuestionResponse::from).toList();
	}

	@GetMapping("/{id}")
	public QuestionResponse question(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
		return QuestionResponse.from(questionService.question(id, principal.userId()));
	}

	public record CreateQuestionRequest(@NotNull Long campusId, @NotNull Long classroomId,
			@NotBlank @Size(max = 100) String category, @NotBlank @Size(max = 200) String title,
			@NotBlank @Size(max = 10000) String content) {
	}

	public record QuestionResponse(Long id, Long authorId, Long campusId, Long classroomId, String category,
			String title, String content, String status, String source, Instant createdAt) {
		static QuestionResponse from(Question question) {
			return new QuestionResponse(question.getId(), question.getAuthor().getId(), question.getCampus().getId(),
					question.getClassroom().getId(), question.getCategory(), question.getTitle(), question.getContent(),
					question.getStatus().name(), question.getSource().name(), question.getCreatedAt());
		}
	}
}
