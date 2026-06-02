package com.taskflow.audit;

import com.taskflow.audit.dto.AuditLogResponseDto;
import com.taskflow.common.enums.AuditAction;
import com.taskflow.common.enums.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDto>> getAllLogs(
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Long performedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(auditLogService.getAllLogs(
                entityType, action, performedBy, page, size));
    }

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogResponseDto>> getLogsByEntity(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(auditLogService.getLogsByEntity(entityType, entityId));
    }
}