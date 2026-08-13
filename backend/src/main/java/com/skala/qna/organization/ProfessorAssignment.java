package com.skala.qna.organization;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "professor_assignments")
public class ProfessorAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "professor_id", nullable = false)
	private User professor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "classroom_id", nullable = false)
	private Classroom classroom;

	protected ProfessorAssignment() {
	}

	public ProfessorAssignment(User professor, Classroom classroom) {
		this.professor = professor;
		this.classroom = classroom;
	}

	public Long getId() {
		return id;
	}

	public User getProfessor() {
		return professor;
	}

	public Classroom getClassroom() {
		return classroom;
	}
}
