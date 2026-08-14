package com.skala.qna.admin;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccessAuditLogRepository extends JpaRepository<StaffAccessAuditLog, Long> {
}
