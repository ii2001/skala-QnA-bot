package com.skala.qna.slack;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRepository;

@Service
@Transactional(readOnly = true)
public class SlackIntegrationService {

	private final SlackUserMappingRepository userMappings;
	private final SlackChannelMappingRepository channelMappings;
	private final UserRepository users;
	private final SlackMessageSender messageSender;

	public SlackIntegrationService(SlackUserMappingRepository userMappings,
			SlackChannelMappingRepository channelMappings, UserRepository users, SlackMessageSender messageSender) {
		this.userMappings = userMappings;
		this.channelMappings = channelMappings;
		this.users = users;
		this.messageSender = messageSender;
	}

	@Transactional
	public SlackUserMapping mapUser(Long userId, String slackUserId) {
		String normalizedSlackUserId = required(slackUserId, "Slack 사용자 ID");
		User user = users.findById(userId).orElseThrow(() -> notFound("사용자를 찾을 수 없습니다."));
		userMappings.findBySlackUserId(normalizedSlackUserId).filter(mapping -> !mapping.getUser().getId().equals(userId))
				.ifPresent(mapping -> { throw conflict("이미 다른 시스템 사용자에 연결된 Slack 사용자 ID입니다."); });

		SlackUserMapping mapping = userMappings.findByUserId(userId)
				.orElseGet(() -> new SlackUserMapping(user, normalizedSlackUserId));
		mapping.updateSlackUserId(normalizedSlackUserId);
		return userMappings.save(mapping);
	}

	@Transactional
	public SlackChannelMapping mapChannel(String scopeType, Long scopeId, String slackChannelId) {
		String normalizedScopeType = required(scopeType, "범위 유형");
		String normalizedSlackChannelId = required(slackChannelId, "Slack 채널 ID");
		channelMappings.findBySlackChannelId(normalizedSlackChannelId)
				.filter(mapping -> !mapping.getScopeType().equals(normalizedScopeType)
						|| !java.util.Objects.equals(mapping.getScopeId(), scopeId))
				.ifPresent(mapping -> { throw conflict("이미 다른 범위에 연결된 Slack 채널 ID입니다."); });

		SlackChannelMapping mapping = channelMappings.findByScopeTypeAndScopeId(normalizedScopeType, scopeId)
				.orElseGet(() -> new SlackChannelMapping(normalizedScopeType, scopeId, normalizedSlackChannelId));
		mapping.update(normalizedScopeType, scopeId, normalizedSlackChannelId);
		return channelMappings.save(mapping);
	}

	public List<SlackChannelMapping> channelMappings() {
		return channelMappings.findAllByOrderByScopeTypeAscScopeIdAsc();
	}

	public Optional<SlackUserMapping> userMapping(Long userId) {
		return userMappings.findByUserId(userId);
	}

	public SlackSendResult sendMessage(String slackChannelId, String text) {
		return messageSender.send(slackChannelId, text);
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public SlackSendResult sendDirectMessage(String slackUserId, String text) {
		return messageSender.sendDirectMessage(slackUserId, text);
	}

	private String required(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + "은(는) 필수입니다.");
		}
		return value.trim();
	}

	private ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}

	private ResponseStatusException conflict(String message) {
		return new ResponseStatusException(HttpStatus.CONFLICT, message);
	}
}
