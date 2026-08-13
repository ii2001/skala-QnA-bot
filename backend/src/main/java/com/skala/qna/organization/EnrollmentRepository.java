package com.skala.qna.organization;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
	Optional<Enrollment> findByStudentId(Long studentId);
}
