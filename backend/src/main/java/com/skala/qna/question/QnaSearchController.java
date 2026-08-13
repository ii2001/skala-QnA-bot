package com.skala.qna.question;

import java.util.List;

import com.skala.qna.auth.UserPrincipal;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qna")
public class QnaSearchController {

	private final QnaSearchService search;

	public QnaSearchController(QnaSearchService search) {
		this.search = search;
	}

	@GetMapping("/search")
	public List<QnaSearchService.Result> search(@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false, defaultValue = "") String q,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) Long campusId,
			@RequestParam(required = false) Long classroomId) {
		return search.search(principal, q, category, campusId, classroomId);
	}
}
