package com.skala.qna.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.skala.qna.organization.User;
import com.skala.qna.organization.UserRepository;
import com.skala.qna.organization.UserRole;

@Service
@Transactional(readOnly = true)
public class StaffAccessService {

	private final StaffAccessRepository accesses;
	private final StaffAccessAuditLogRepository auditLogs;
	private final UserRepository users;

	public StaffAccessService(StaffAccessRepository accesses, StaffAccessAuditLogRepository auditLogs,
			UserRepository users) {
		this.accesses = accesses;
		this.auditLogs = auditLogs;
		this.users = users;
	}

	public List<StaffAccess> list() {
		return accesses.findAllByOrderByEmailAsc();
	}

	public StaffAccess activeAccess(String email) {
		return accesses.findByEmailIgnoreCaseAndActiveTrue(email).orElse(null);
	}

	@Transactional
	public StaffAccess create(String email, UserRole role, String note, Long actorId) {
		validateRole(role);
		String normalizedEmail = email(email);
		if (accesses.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
			throw conflict("이미 등록된 Staff 이메일입니다.");
		}
		StaffAccess access = accesses.save(new StaffAccess(normalizedEmail, role, note));
		log("CREATED", access, actorId);
		return access;
	}

	@Transactional
	public StaffAccess update(Long id, UserRole role, String note, boolean active, Long actorId) {
		validateRole(role);
		StaffAccess access = access(id);
		access.update(role, note, active);
		log("UPDATED", access, actorId);
		return access;
	}

	@Transactional
	public void deactivate(Long id, Long actorId) {
		StaffAccess access = access(id);
		access.update(access.getExpectedRole(), access.getNote(), false);
		log("DEACTIVATED", access, actorId);
	}

	private void log(String action, StaffAccess access, Long actorId) {
		User actor = actorId == null ? null : users.findById(actorId).orElse(null);
		auditLogs.save(new StaffAccessAuditLog(action, access.getEmail(), access.getExpectedRole().name(), actor));
	}

	private StaffAccess access(Long id) {
		return accesses.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff 허용 목록을 찾을 수 없습니다."));
	}

	private void validateRole(UserRole role) {
		if (role == null || role == UserRole.STUDENT) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff 역할은 PROFESSOR 또는 ADMIN이어야 합니다.");
		}
	}

	private String email(String value) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일은 필수입니다.");
		}
		return value.trim().toLowerCase(java.util.Locale.ROOT);
	}

	private ResponseStatusException conflict(String message) {
		return new ResponseStatusException(HttpStatus.CONFLICT, message);
	}
}
