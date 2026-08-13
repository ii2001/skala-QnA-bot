package com.skala.qna.organization;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
	List<Classroom> findAllByCampusIdOrderByName(Long campusId);
}
