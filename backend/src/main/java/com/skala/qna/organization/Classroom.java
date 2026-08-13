package com.skala.qna.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "classrooms")
public class Classroom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campus_id", nullable = false)
	private Campus campus;

	@Column(nullable = false, length = 100)
	private String name;

	protected Classroom() {
	}

	public Classroom(Campus campus, String name) {
		this.campus = campus;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Campus getCampus() {
		return campus;
	}

	public String getName() {
		return name;
	}

	public void rename(String name) {
		this.name = name;
	}
}
