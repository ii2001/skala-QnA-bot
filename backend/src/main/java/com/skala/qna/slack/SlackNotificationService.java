package com.skala.qna.slack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.organization.ProfessorAssignmentRepository;
import com.skala.qna.organization.User;
import com.skala.qna.question.Answer;
import com.skala.qna.question.AnswerScopeResolver;
import com.skala.qna.question.AnswerVisibility;
import com.skala.qna.question.Question;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SlackNotificationService {

	private static final Logger log = LoggerFactory.getLogger(SlackNotificationService.class);

	private final ProfessorAssignmentRepository assignments;
	private final SlackIntegrationService slack;
	private final AnswerScopeResolver scopes;

	public SlackNotificationService(ProfessorAssignmentRepository assignments, SlackIntegrationService slack,
			AnswerScopeResolver scopes) {
		this.assignments = assignments;
		this.slack = slack;
		this.scopes = scopes;
	}

	public void notifyNewQuestion(Question question) {
		try {
			String message = "새 질문이 등록되었습니다.\n제목: " + question.getTitle() + "\n내용: " + question.getContent();
			assignments.findAllByClassroomId(question.getClassroom().getId()).forEach(assignment ->
				notifyUser(assignment.getProfessor(), question.getId(), "new-question", message));
		} catch (RuntimeException exception) {
			log.warn("Slack notification failed: event=new-question, questionId={}, exception={}", question.getId(),
					exception.getClass().getSimpleName());
		}
	}

	public void notifyAnswer(Answer answer) {
		Question question = answer.getQuestion();
		try {
			String message = "질문에 답변이 등록되었습니다.\n질문: " + question.getTitle() + "\n답변: " + answer.getContent();
			notifyUser(question.getAuthor(), question.getId(), "answer", message);
		} catch (RuntimeException exception) {
			log.warn("Slack notification failed: event=answer, exception={}", exception.getClass().getSimpleName());
		}
		notifyAnswerChannels(answer, question);
	}

	private void notifyAnswerChannels(Answer answer, Question question) {
		if (answer.getVisibility() == AnswerVisibility.PRIVATE) {
			return;
		}
		try {
			List<String> channelIds = channelIds(question, answer.getVisibility());
			if (channelIds.isEmpty()) {
				log.warn("Slack channel broadcast skipped: event=answer-channel, questionId={}, visibility={}, reason=mapping-missing",
						question.getId(), answer.getVisibility());
				return;
			}
			String message = "SKALA 답변이 등록되었습니다.\n질문 제목: " + question.getTitle() + "\n질문 내용: "
					+ question.getContent() + "\n답변: " + answer.getContent();
			for (String channelId : channelIds) {
				try {
					SlackSendResult result = slack.sendMessage(channelId, message);
					if (!result.sent()) {
						log.warn("Slack channel broadcast failed: event=answer-channel, questionId={}, visibility={}, reason={}",
								question.getId(), answer.getVisibility(), result.error());
					}
				} catch (RuntimeException exception) {
					log.warn("Slack channel broadcast failed: event=answer-channel, questionId={}, visibility={}, exception={}",
							question.getId(), answer.getVisibility(), exception.getClass().getSimpleName());
				}
			}
		} catch (RuntimeException exception) {
			log.warn("Slack channel broadcast failed: event=answer-channel, questionId={}, visibility={}, exception={}",
					question.getId(), answer.getVisibility(), exception.getClass().getSimpleName());
		}
	}

	private List<String> channelIds(Question question, AnswerVisibility visibility) {
		List<SlackChannelMapping> mappings = slack.channelMappings();
		Set<String> channelIds = new LinkedHashSet<>();
		switch (visibility) {
		case CLASS -> addChannels(channelIds, mappings, "CLASS", Set.of(question.getClassroom().getId()));
		case CAMPUS -> {
			Set<Long> campusIds = Set.of(question.getCampus().getId());
			addChannels(channelIds, mappings, "CAMPUS", campusIds);
			if (channelIds.isEmpty()) {
				addChannels(channelIds, mappings, "CLASS", scopes.resolve(question, visibility).classroomIds());
			}
		}
		case GLOBAL -> mappings.stream()
				.filter(mapping -> Set.of("CLASS", "CAMPUS", "GLOBAL").contains(mapping.getScopeType()))
				.map(SlackChannelMapping::getSlackChannelId)
				.filter(channelId -> channelId != null && !channelId.isBlank())
				.forEach(channelIds::add);
		case PRIVATE -> {
		}
		}
		return List.copyOf(channelIds);
	}

	private void addChannels(Set<String> channelIds, List<SlackChannelMapping> mappings, String scopeType,
			Set<Long> scopeIds) {
		mappings.stream()
				.filter(mapping -> scopeType.equals(mapping.getScopeType()) && scopeIds.contains(mapping.getScopeId()))
				.map(SlackChannelMapping::getSlackChannelId)
				.filter(channelId -> channelId != null && !channelId.isBlank())
				.forEach(channelIds::add);
	}

	private void notifyUser(User user, Long questionId, String event, String message) {
		try {
			var mapping = slack.userMapping(user.getId());
			if (mapping.isEmpty()) {
				log.warn("Slack DM skipped: event={}, questionId={}, recipientUserId={}, reason=mapping-missing", event,
						questionId, user.getId());
				return;
			}
			SlackSendResult result = slack.sendDirectMessage(mapping.get().getSlackUserId(), message);
			if (!result.sent()) {
				log.warn("Slack DM failed: event={}, questionId={}, recipientUserId={}, reason={}", event, questionId,
						user.getId(), result.error());
			}
		} catch (RuntimeException exception) {
			log.warn("Slack DM failed: event={}, questionId={}, recipientUserId={}, exception={}", event, questionId,
					user.getId(), exception.getClass().getSimpleName());
		}
	}
}
