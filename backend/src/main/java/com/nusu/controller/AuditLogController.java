package com.nusu.controller;

import com.nusu.model.AuditLog;
import com.nusu.repository.AuditLogRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogs;

    public AuditLogController(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping
    public List<AuditLog> list() {
        return auditLogs.findTop50ByOrderByTimestampDesc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditLog create(@RequestBody Map<String, String> body) {
        String action = body.getOrDefault("action", "USER_ACTION");
        String actor = body.getOrDefault("actor", "Mission Control");
        String details = body.getOrDefault("details", "Không có chi tiết");
        return auditLogs.save(new AuditLog(action, actor, details));
    }
}
