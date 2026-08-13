package com.skala.qna.slack;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackChannelMappingRepository extends JpaRepository<SlackChannelMapping, Long> {

	Optional<SlackChannelMapping> findByScopeTypeAndScopeId(String scopeType, Long scopeId);

	Optional<SlackChannelMapping> findBySlackChannelId(String slackChannelId);

	List<SlackChannelMapping> findAllByOrderByScopeTypeAscScopeIdAsc();
}
