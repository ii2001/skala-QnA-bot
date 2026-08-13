package com.skala.qna.question;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
	boolean existsByQuestionId(Long questionId);

	Optional<Answer> findByQuestionId(Long questionId);

	@EntityGraph(attributePaths = { "question", "question.author", "question.campus", "question.classroom" })
	@Query("""
			select a from Answer a
			where a.question.status = 'ANSWERED'
			and (lower(a.question.title) like lower(concat('%', :query, '%'))
			or lower(a.question.content) like lower(concat('%', :query, '%'))
			or lower(a.content) like lower(concat('%', :query, '%'))
			or lower(a.question.category) like lower(concat('%', :query, '%')))
			order by a.createdAt desc
			""")
	List<Answer> search(@Param("query") String query);
}
