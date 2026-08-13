package com.skala.qna.question;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

	List<Question> findAllByAuthorIdOrderByCreatedAtDesc(Long authorId);

	Optional<Question> findByIdAndAuthorId(Long id, Long authorId);
}
