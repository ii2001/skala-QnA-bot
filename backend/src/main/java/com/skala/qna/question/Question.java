package com.skala.qna.question;

import java.time.Instant;

import com.skala.qna.organization.Campus;
import com.skala.qna.organization.Classroom;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "questions")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campus_id", nullable = false)
	private Campus campus;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "classroom_id", nullable = false)
	private Classroom classroom;

	@Column(nullable = false, length = 100)
	private String category;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuestionStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuestionSource source;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Question() {
	}

	public Question(User author, Campus campus, Classroom classroom, String category, String title, String content) {
		this.author = author;
		this.campus = campus;
		this.classroom = classroom;
		this.category = category;
		this.title = title;
		this.content = content;
		this.status = QuestionStatus.OPEN;
		this.source = QuestionSource.WEB;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getAuthor() {
		return author;
	}

	public Campus getCampus() {
		return campus;
	}

	public Classroom getClassroom() {
		return classroom;
	}

	public String getCategory() {
		return category;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public QuestionStatus getStatus() {
		return status;
	}

	public void markAnswered() {
		this.status = QuestionStatus.ANSWERED;
	}

	public QuestionSource getSource() {
		return source;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

enum QuestionStatus {
	OPEN, ANSWERED
}

enum QuestionSource {
	WEB
}
