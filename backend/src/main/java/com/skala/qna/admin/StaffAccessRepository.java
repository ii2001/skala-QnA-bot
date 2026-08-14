package com.skala.qna.admin;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccessRepository extends JpaRepository<StaffAccess, Long> {
	Optional<StaffAccess> findByEmailIgnoreCase(String email);

	Optional<StaffAccess> findByEmailIgnoreCaseAndActiveTrue(String email);

	List<StaffAccess> findAllByOrderByEmailAsc();
}
