package com.skala.qna.question;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

	List<Question> findAllByAuthorIdOrderByCreatedAtDesc(Long authorId);

	Optional<Question> findByIdAndAuthorId(Long id, Long authorId);

	@EntityGraph(attributePaths = { "author", "campus", "classroom" })
	List<Question> findAllByClassroomIdInOrderByCreatedAtDesc(Iterable<Long> classroomIds);

	@EntityGraph(attributePaths = { "author", "campus", "classroom" })
	Optional<Question> findByIdAndClassroomIdIn(Long id, Iterable<Long> classroomIds);
}
