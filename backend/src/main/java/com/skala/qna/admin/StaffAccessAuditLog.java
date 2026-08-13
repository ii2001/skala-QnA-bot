package com.skala.qna.admin;

import java.time.Instant;

import com.skala.qna.organization.User;

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
@Table(name = "staff_access_audit_logs")
public class StaffAccessAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String action;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(name = "expected_role", length = 20)
	private String expectedRole;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id")
	private User actor;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected StaffAccessAuditLog() {
	}

	public StaffAccessAuditLog(String action, String email, String expectedRole, User actor) {
		this.action = action;
		this.email = email;
		this.expectedRole = expectedRole;
		this.actor = actor;
		this.createdAt = Instant.now();
	}
}
