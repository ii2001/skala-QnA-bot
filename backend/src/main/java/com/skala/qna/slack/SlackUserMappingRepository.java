package com.skala.qna.slack;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackUserMappingRepository extends JpaRepository<SlackUserMapping, Long> {

	Optional<SlackUserMapping> findByUserId(Long userId);

	Optional<SlackUserMapping> findBySlackUserId(String slackUserId);
}
