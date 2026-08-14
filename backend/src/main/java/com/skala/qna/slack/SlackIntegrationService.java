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
import com.skala.qna.organization.CampusRepository;
import com.skala.qna.organization.ClassroomRepository;

@Service
@Transactional(readOnly = true)
public class SlackIntegrationService {

	private final SlackUserMappingRepository userMappings;
	private final SlackChannelMappingRepository channelMappings;
	private final UserRepository users;
	private final CampusRepository campuses;
	private final ClassroomRepository classrooms;
	private final SlackMessageSender messageSender;

	public SlackIntegrationService(SlackUserMappingRepository userMappings,
			SlackChannelMappingRepository channelMappings, UserRepository users, CampusRepository campuses,
			ClassroomRepository classrooms, SlackMessageSender messageSender) {
		this.userMappings = userMappings;
		this.channelMappings = channelMappings;
		this.users = users;
		this.campuses = campuses;
		this.classrooms = classrooms;
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
		String normalizedScopeType = validateScope(scopeType, scopeId);
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

	public List<SlackUserMapping> userMappings() {
		return userMappings.findAll();
	}

	@Transactional
	public void deleteUserMapping(Long id) {
		userMappings.delete(userMappings.findById(id)
				.orElseThrow(() -> notFound("Slack 사용자 매핑을 찾을 수 없습니다.")));
	}

	@Transactional
	public SlackChannelMapping updateChannel(Long id, String scopeType, Long scopeId, String slackChannelId) {
		SlackChannelMapping mapping = channelMappings.findById(id)
				.orElseThrow(() -> notFound("Slack 채널 매핑을 찾을 수 없습니다."));
		String normalizedScopeType = validateScope(scopeType, scopeId);
		String normalizedSlackChannelId = required(slackChannelId, "Slack 채널 ID");
		channelMappings.findBySlackChannelId(normalizedSlackChannelId)
				.filter(other -> !other.getId().equals(id))
				.ifPresent(other -> { throw conflict("이미 다른 범위에 연결된 Slack 채널 ID입니다."); });
		mapping.update(normalizedScopeType, scopeId, normalizedSlackChannelId);
		return mapping;
	}

	@Transactional
	public void deleteChannel(Long id) {
		channelMappings.delete(channelMappings.findById(id)
				.orElseThrow(() -> notFound("Slack 채널 매핑을 찾을 수 없습니다.")));
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

	private String validateScope(String scopeType, Long scopeId) {
		String normalized = required(scopeType, "범위 유형").toUpperCase(java.util.Locale.ROOT);
		switch (normalized) {
		case "CLASS" -> {
			if (scopeId == null || !classrooms.existsById(scopeId)) throw notFound("클래스를 찾을 수 없습니다.");
		}
		case "CAMPUS" -> {
			if (scopeId == null || !campuses.existsById(scopeId)) throw notFound("캠퍼스를 찾을 수 없습니다.");
		}
		case "GLOBAL" -> {
			if (scopeId != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GLOBAL 범위에는 scopeId를 사용할 수 없습니다.");
		}
		default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "범위 유형은 CLASS, CAMPUS 또는 GLOBAL이어야 합니다.");
		}
		return normalized;
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
