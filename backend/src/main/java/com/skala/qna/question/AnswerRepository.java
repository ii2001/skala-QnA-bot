package com.skala.qna.question;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
	boolean existsByQuestionId(Long questionId);

	Optional<Answer> findByQuestionId(Long questionId);
}
