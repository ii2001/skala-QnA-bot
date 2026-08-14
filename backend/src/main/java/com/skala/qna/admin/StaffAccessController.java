package com.skala.qna.admin;

import java.time.Instant;
import java.util.List;

import com.skala.qna.auth.UserPrincipal;
import com.skala.qna.organization.UserRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/staff-access")
@PreAuthorize("hasRole('ADMIN')")
public class StaffAccessController {

	private final StaffAccessService staff;

	public StaffAccessController(StaffAccessService staff) {
		this.staff = staff;
	}

	@GetMapping
	public List<StaffAccessResponse> list() {
		return staff.list().stream().map(StaffAccessResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public StaffAccessResponse create(@Valid @RequestBody CreateRequest request,
			@AuthenticationPrincipal UserPrincipal actor) {
		return StaffAccessResponse.from(staff.create(request.email(), request.expectedRole(), request.note(), actor.userId()));
	}

	@PutMapping("/{id}")
	public StaffAccessResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request,
			@AuthenticationPrincipal UserPrincipal actor) {
		return StaffAccessResponse.from(staff.update(id, request.expectedRole(), request.note(), request.active(), actor.userId()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
		staff.deactivate(id, actor.userId());
	}

	public record CreateRequest(@Email String email, @NotNull UserRole expectedRole,
			@Size(max = 500) String note) {
	}

	public record UpdateRequest(@NotNull UserRole expectedRole, @Size(max = 500) String note, boolean active) {
	}

	public record StaffAccessResponse(Long id, String email, UserRole expectedRole, boolean active, String note,
			Instant updatedAt) {
		static StaffAccessResponse from(StaffAccess access) {
			return new StaffAccessResponse(access.getId(), access.getEmail(), access.getExpectedRole(), access.isActive(),
					access.getNote(), access.getUpdatedAt());
		}
	}
}
