package com.kws.backend.backend.service;

import com.kws.backend.backend.model.AuditLog;
import com.kws.backend.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logAction(String username, String actionType, String entityType, String entityId, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username != null ? username : "SYSTEM");
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress("127.0.0.1");

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("[AUDIT] [{}] User: {}, Entity: {} ({}), Description: {}",
                actionType, username, entityType, entityId, description);
        return saved;
    }

    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByAction(String actionType) {
        return auditLogRepository.findByActionType(actionType);
    }
}
