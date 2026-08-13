package com.skala.qna.admin;

import java.time.Instant;

import com.skala.qna.organization.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_access")
public class StaffAccess {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(name = "expected_role", nullable = false, length = 20)
	private UserRole expectedRole;

	@Column(nullable = false)
	private boolean active;

	@Column(length = 500)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StaffAccess() {
	}

	public StaffAccess(String email, UserRole expectedRole, String note) {
		this.email = email;
		this.expectedRole = expectedRole;
		this.active = true;
		this.note = note;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public UserRole getExpectedRole() {
		return expectedRole;
	}

	public boolean isActive() {
		return active;
	}

	public String getNote() {
		return note;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void update(UserRole expectedRole, String note, boolean active) {
		this.expectedRole = expectedRole;
		this.note = note;
		this.active = active;
		this.updatedAt = Instant.now();
	}
}
