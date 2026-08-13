package com.skala.qna.organization;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorAssignmentRepository extends JpaRepository<ProfessorAssignment, Long> {
	List<ProfessorAssignment> findAllByProfessorId(Long professorId);

	boolean existsByProfessorIdAndClassroomId(Long professorId, Long classroomId);
}
