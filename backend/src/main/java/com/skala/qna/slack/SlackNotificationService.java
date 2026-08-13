package com.skala.qna.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.organization.ProfessorAssignmentRepository;
import com.skala.qna.organization.User;
import com.skala.qna.question.Answer;
import com.skala.qna.question.Question;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SlackNotificationService {

	private static final Logger log = LoggerFactory.getLogger(SlackNotificationService.class);

	private final ProfessorAssignmentRepository assignments;
	private final SlackIntegrationService slack;

	public SlackNotificationService(ProfessorAssignmentRepository assignments, SlackIntegrationService slack) {
		this.assignments = assignments;
		this.slack = slack;
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
		try {
			Question question = answer.getQuestion();
			String message = "질문에 답변이 등록되었습니다.\n질문: " + question.getTitle() + "\n답변: " + answer.getContent();
			notifyUser(question.getAuthor(), question.getId(), "answer", message);
		} catch (RuntimeException exception) {
			log.warn("Slack notification failed: event=answer, exception={}", exception.getClass().getSimpleName());
		}
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
