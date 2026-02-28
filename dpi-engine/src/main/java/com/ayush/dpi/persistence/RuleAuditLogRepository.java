package com.ayush.dpi.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleAuditLogRepository extends JpaRepository<RuleAuditLogEntity, Long> {
}
