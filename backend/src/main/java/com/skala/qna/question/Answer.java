package com.skala.qna.question;

import java.time.Instant;

import com.skala.qna.organization.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "answers")
public class Answer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false, unique = true)
	private Question question;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "professor_id", nullable = false)
	private User professor;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AnswerVisibility visibility;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Answer() {
	}

	public Answer(Question question, User professor, String content, AnswerVisibility visibility) {
		this.question = question;
		this.professor = professor;
		this.content = content;
		this.visibility = visibility;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Question getQuestion() {
		return question;
	}

	public User getProfessor() {
		return professor;
	}

	public String getContent() {
		return content;
	}

	public AnswerVisibility getVisibility() {
		return visibility;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
