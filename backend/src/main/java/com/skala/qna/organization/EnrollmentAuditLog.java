package com.skala.qna.organization;

import java.time.Instant;

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
@Table(name = "enrollment_audit_logs")
public class EnrollmentAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private User student;

	@Column(name = "previous_campus_id")
	private Long previousCampusId;

	@Column(name = "previous_classroom_id")
	private Long previousClassroomId;

	@Column(name = "new_campus_id", nullable = false)
	private Long newCampusId;

	@Column(name = "new_classroom_id", nullable = false)
	private Long newClassroomId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected EnrollmentAuditLog() {
	}

	public EnrollmentAuditLog(User student, Long previousCampusId, Long previousClassroomId, Long newCampusId,
			Long newClassroomId) {
		this.student = student;
		this.previousCampusId = previousCampusId;
		this.previousClassroomId = previousClassroomId;
		this.newCampusId = newCampusId;
		this.newClassroomId = newClassroomId;
		this.createdAt = Instant.now();
	}
}
