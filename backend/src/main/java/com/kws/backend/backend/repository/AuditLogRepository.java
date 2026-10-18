package com.kws.backend.backend.repository;

import com.kws.backend.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByOrderByTimestampDesc();
    List<AuditLog> findByActionType(String actionType);
    List<AuditLog> findByUsername(String username);
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
