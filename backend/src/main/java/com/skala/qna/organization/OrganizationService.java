package com.skala.qna.organization;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

	private final CampusRepository campuses;
	private final ClassroomRepository classrooms;
	private final UserRepository users;
	private final EnrollmentRepository enrollments;
	private final ProfessorAssignmentRepository assignments;
	private final PasswordEncoder passwordEncoder;

	public OrganizationService(CampusRepository campuses, ClassroomRepository classrooms, UserRepository users,
			EnrollmentRepository enrollments, ProfessorAssignmentRepository assignments,
			PasswordEncoder passwordEncoder) {
		this.campuses = campuses;
		this.classrooms = classrooms;
		this.users = users;
		this.enrollments = enrollments;
		this.assignments = assignments;
		this.passwordEncoder = passwordEncoder;
	}

	public List<Campus> campuses() {
		return campuses.findAll();
	}

	@Transactional
	public Campus createCampus(String name) {
		return campuses.save(new Campus(name));
	}

	@Transactional
	public Campus updateCampus(Long id, String name) {
		Campus campus = campus(id);
		campus.rename(name);
		return campus;
	}

	@Transactional
	public void deleteCampus(Long id) {
		campuses.delete(campus(id));
	}

	public List<Classroom> classrooms(Long campusId) {
		campus(campusId);
		return classrooms.findAllByCampusIdOrderByName(campusId);
	}

	@Transactional
	public Classroom createClassroom(Long campusId, String name) {
		return classrooms.save(new Classroom(campus(campusId), name));
	}

	@Transactional
	public Classroom updateClassroom(Long id, String name) {
		Classroom classroom = classroom(id);
		classroom.rename(name);
		return classroom;
	}

	@Transactional
	public void deleteClassroom(Long id) {
		classrooms.delete(classroom(id));
	}

	public List<User> users() {
		return users.findAll();
	}

	@Transactional
	public User createUser(String name, String email, UserRole role) {
		return createUser(name, email, role, null);
	}

	@Transactional
	public User createUser(String name, String email, UserRole role, String rawPassword) {
		return users.save(new User(name, email, role, hash(rawPassword)));
	}

	@Transactional
	public User updateUser(Long id, String name, String email, UserRole role) {
		return updateUser(id, name, email, role, null);
	}

	@Transactional
	public User updateUser(Long id, String name, String email, UserRole role, String rawPassword) {
		User user = user(id);
		user.update(name, email, role, rawPassword == null ? user.getPasswordHash() : hash(rawPassword));
		return user;
	}

	@Transactional
	public User bootstrapAdmin(String name, String email, String rawPassword) {
		User user = users.findByEmail(email).orElseGet(() -> new User(name, email, UserRole.ADMIN));
		user.update(name, email, UserRole.ADMIN, passwordEncoder.encode(rawPassword));
		return users.save(user);
	}

	@Transactional
	public void deleteUser(Long id) {
		users.delete(user(id));
	}

	public Enrollment enrollment(Long studentId) {
		return enrollments.findByStudentId(studentId)
				.orElseThrow(() -> notFound("학생 등록 정보를 찾을 수 없습니다."));
	}

	@Transactional
	public Enrollment enroll(Long studentId, Long campusId, Long classroomId) {
		User student = userWithRole(studentId, UserRole.STUDENT);
		Campus campus = campus(campusId);
		Classroom classroom = classroom(classroomId);
		if (!classroom.getCampus().getId().equals(campus.getId())) {
			throw badRequest("클래스가 선택한 캠퍼스에 속하지 않습니다.");
		}
		return enrollments.findByStudentId(studentId).map(enrollment -> {
			enrollment.moveTo(campus, classroom);
			return enrollment;
		}).orElseGet(() -> enrollments.save(new Enrollment(student, campus, classroom)));
	}

	@Transactional
	public void deleteEnrollment(Long studentId) {
		enrollments.delete(enrollment(studentId));
	}

	public List<ProfessorAssignment> assignments(Long professorId) {
		userWithRole(professorId, UserRole.PROFESSOR);
		return assignments.findAllByProfessorId(professorId);
	}

	@Transactional
	public ProfessorAssignment assign(Long professorId, Long classroomId) {
		User professor = userWithRole(professorId, UserRole.PROFESSOR);
		Classroom classroom = classroom(classroomId);
		if (assignments.existsByProfessorIdAndClassroomId(professorId, classroomId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 담당 중인 클래스입니다.");
		}
		return assignments.save(new ProfessorAssignment(professor, classroom));
	}

	@Transactional
	public void deleteAssignment(Long id) {
		assignments.delete(assignments.findById(id)
				.orElseThrow(() -> notFound("교수 배정 정보를 찾을 수 없습니다.")));
	}

	private Campus campus(Long id) {
		return campuses.findById(id).orElseThrow(() -> notFound("캠퍼스를 찾을 수 없습니다."));
	}

	private Classroom classroom(Long id) {
		return classrooms.findById(id).orElseThrow(() -> notFound("클래스를 찾을 수 없습니다."));
	}

	private User user(Long id) {
		return users.findById(id).orElseThrow(() -> notFound("사용자를 찾을 수 없습니다."));
	}

	private User userWithRole(Long id, UserRole role) {
		User user = user(id);
		if (user.getRole() != role) {
			throw badRequest(role == UserRole.STUDENT ? "학생만 등록할 수 있습니다." : "교수만 배정할 수 있습니다.");
		}
		return user;
	}

	private ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}

	private ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

	private String hash(String rawPassword) {
		return rawPassword == null ? null : passwordEncoder.encode(rawPassword);
	}
}
