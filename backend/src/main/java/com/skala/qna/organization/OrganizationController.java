package com.skala.qna.organization;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
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
@RequestMapping("/api")
public class OrganizationController {

	private final OrganizationService organization;

	public OrganizationController(OrganizationService organization) {
		this.organization = organization;
	}

	@GetMapping("/campuses")
	public List<CampusResponse> campuses() {
		return organization.campuses().stream().map(CampusResponse::from).toList();
	}

	@PostMapping("/campuses")
	@ResponseStatus(HttpStatus.CREATED)
	public CampusResponse createCampus(@Valid @RequestBody NameRequest request) {
		return CampusResponse.from(organization.createCampus(request.name()));
	}

	@PutMapping("/campuses/{id}")
	public CampusResponse updateCampus(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
		return CampusResponse.from(organization.updateCampus(id, request.name()));
	}

	@DeleteMapping("/campuses/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCampus(@PathVariable Long id) {
		organization.deleteCampus(id);
	}

	@GetMapping("/campuses/{campusId}/classrooms")
	public List<ClassroomResponse> classrooms(@PathVariable Long campusId) {
		return organization.classrooms(campusId).stream().map(ClassroomResponse::from).toList();
	}

	@PostMapping("/campuses/{campusId}/classrooms")
	@ResponseStatus(HttpStatus.CREATED)
	public ClassroomResponse createClassroom(@PathVariable Long campusId, @Valid @RequestBody NameRequest request) {
		return ClassroomResponse.from(organization.createClassroom(campusId, request.name()));
	}

	@PutMapping("/classrooms/{id}")
	public ClassroomResponse updateClassroom(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
		return ClassroomResponse.from(organization.updateClassroom(id, request.name()));
	}

	@DeleteMapping("/classrooms/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteClassroom(@PathVariable Long id) {
		organization.deleteClassroom(id);
	}

	@GetMapping("/users")
	public List<UserResponse> users() {
		return organization.users().stream().map(UserResponse::from).toList();
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse createUser(@Valid @RequestBody UserRequest request) {
		return UserResponse.from(organization.createUser(request.name(), request.email(), request.role()));
	}

	@PutMapping("/users/{id}")
	public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
		return UserResponse.from(organization.updateUser(id, request.name(), request.email(), request.role()));
	}

	@DeleteMapping("/users/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(@PathVariable Long id) {
		organization.deleteUser(id);
	}

	@GetMapping("/students/{studentId}/enrollment")
	public EnrollmentResponse enrollment(@PathVariable Long studentId) {
		return EnrollmentResponse.from(organization.enrollment(studentId));
	}

	@PutMapping("/students/{studentId}/enrollment")
	public EnrollmentResponse enroll(@PathVariable Long studentId, @Valid @RequestBody EnrollmentRequest request) {
		return EnrollmentResponse.from(organization.enroll(studentId, request.campusId(), request.classroomId()));
	}

	@DeleteMapping("/students/{studentId}/enrollment")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteEnrollment(@PathVariable Long studentId) {
		organization.deleteEnrollment(studentId);
	}

	@GetMapping("/professors/{professorId}/assignments")
	public List<AssignmentResponse> assignments(@PathVariable Long professorId) {
		return organization.assignments(professorId).stream().map(AssignmentResponse::from).toList();
	}

	@PostMapping("/professors/{professorId}/assignments")
	@ResponseStatus(HttpStatus.CREATED)
	public AssignmentResponse assign(@PathVariable Long professorId, @Valid @RequestBody AssignmentRequest request) {
		return AssignmentResponse.from(organization.assign(professorId, request.classroomId()));
	}

	@DeleteMapping("/professor-assignments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAssignment(@PathVariable Long id) {
		organization.deleteAssignment(id);
	}

	public record NameRequest(@NotBlank String name) {
	}

	public record UserRequest(@NotBlank String name, @NotBlank @Email String email, @NotNull UserRole role) {
	}

	public record EnrollmentRequest(@NotNull Long campusId, @NotNull Long classroomId) {
	}

	public record AssignmentRequest(@NotNull Long classroomId) {
	}

	public record CampusResponse(Long id, String name) {
		static CampusResponse from(Campus campus) {
			return new CampusResponse(campus.getId(), campus.getName());
		}
	}

	public record ClassroomResponse(Long id, Long campusId, String name) {
		static ClassroomResponse from(Classroom classroom) {
			return new ClassroomResponse(classroom.getId(), classroom.getCampus().getId(), classroom.getName());
		}
	}

	public record UserResponse(Long id, String name, String email, UserRole role) {
		static UserResponse from(User user) {
			return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
		}
	}

	public record EnrollmentResponse(Long id, Long studentId, Long campusId, Long classroomId) {
		static EnrollmentResponse from(Enrollment enrollment) {
			return new EnrollmentResponse(enrollment.getId(), enrollment.getStudent().getId(),
					enrollment.getCampus().getId(), enrollment.getClassroom().getId());
		}
	}

	public record AssignmentResponse(Long id, Long professorId, Long campusId, Long classroomId) {
		static AssignmentResponse from(ProfessorAssignment assignment) {
			Classroom classroom = assignment.getClassroom();
			return new AssignmentResponse(assignment.getId(), assignment.getProfessor().getId(),
					classroom.getCampus().getId(), classroom.getId());
		}
	}
}
