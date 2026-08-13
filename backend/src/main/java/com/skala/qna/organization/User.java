package com.skala.qna.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(nullable = false)
	private boolean active;

	protected User() {
	}

	public User(String name, String email, UserRole role) {
		this(name, email, role, null);
	}

	public User(String name, String email, UserRole role, String passwordHash) {
		this.name = name;
		this.email = email;
		this.role = role;
		this.passwordHash = passwordHash;
		this.active = true;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public UserRole getRole() {
		return role;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public boolean isActive() {
		return active;
	}

	public void update(String name, String email, UserRole role) {
		update(name, email, role, passwordHash);
	}

	public void update(String name, String email, UserRole role, String passwordHash) {
		update(name, email, role, passwordHash, active);
	}

	public void update(String name, String email, UserRole role, String passwordHash, boolean active) {
		this.name = name;
		this.email = email;
		this.role = role;
		this.passwordHash = passwordHash;
		this.active = active;
	}
}
