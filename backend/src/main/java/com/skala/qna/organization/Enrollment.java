package com.skala.qna.organization;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "enrollments")
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false, unique = true)
	private User student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campus_id", nullable = false)
	private Campus campus;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "classroom_id", nullable = false)
	private Classroom classroom;

	protected Enrollment() {
	}

	public Enrollment(User student, Campus campus, Classroom classroom) {
		this.student = student;
		this.campus = campus;
		this.classroom = classroom;
	}

	public Long getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public Campus getCampus() {
		return campus;
	}

	public Classroom getClassroom() {
		return classroom;
	}

	public void moveTo(Campus campus, Classroom classroom) {
		this.campus = campus;
		this.classroom = classroom;
	}
}
