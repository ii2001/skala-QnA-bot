package com.skala.qna.organization;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrganizationExceptionHandler {

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleConflict() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "중복 값이 있거나 사용 중인 데이터입니다.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요.");
		problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
				.collect(java.util.stream.Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(),
						(first, ignored) -> first)));
		return problem;
	}
}
