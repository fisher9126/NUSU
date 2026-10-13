package com.nusu.service;

import com.nusu.model.AuditLog;
import com.nusu.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    public void log(String action, String actor, String details) {
        auditLogs.save(new AuditLog(action, (actor != null && !actor.isBlank()) ? actor : "Mission Control", details));
    }
}
