package com.taskflow.audit;

import com.taskflow.audit.dto.AuditLogResponseDto;
import com.taskflow.common.enums.AuditAction;
import com.taskflow.common.enums.EntityType;
import com.taskflow.user.User;
import com.taskflow.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    // ── Log Action ────────────────────────────────────────────────
    @Transactional
    public void log(EntityType entityType, Long entityId,
                    AuditAction action, String changedFields, Long userId) {
        User user = userService.findUserById(userId);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setChangedFields(changedFields);
        auditLog.setPerformedBy(user);
        auditLog.setPerformedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
    }

    // ── Get All Logs ──────────────────────────────────────────────
    @Transactional
    public Page<AuditLogResponseDto> getAllLogs(EntityType entityType,
                                                AuditAction action,
                                                Long performedBy,
                                                int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("performedAt").descending());

        if (entityType != null) {
            return auditLogRepository.findByEntityType(entityType, pageable)
                    .map(this::toResponseDto);
        }

        if (action != null) {
            return auditLogRepository.findByAction(action, pageable)
                    .map(this::toResponseDto);
        }

        if (performedBy != null) {
            return auditLogRepository.findByPerformedBy_Id(performedBy, pageable)
                    .map(this::toResponseDto);
        }

        return auditLogRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    // ── Get Logs By Entity ────────────────────────────────────────
    @Transactional
    public java.util.List<AuditLogResponseDto> getLogsByEntity(
            EntityType entityType, Long entityId) {
        return auditLogRepository
                .findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    // ── Internal Mapper ───────────────────────────────────────────
    private AuditLogResponseDto toResponseDto(AuditLog auditLog) {
        AuditLogResponseDto dto = new AuditLogResponseDto();
        dto.setId(auditLog.getId());
        dto.setEntityType(auditLog.getEntityType());
        dto.setEntityId(auditLog.getEntityId());
        dto.setAction(auditLog.getAction());
        dto.setChangedFields(auditLog.getChangedFields());
        dto.setPerformedBy(auditLog.getPerformedBy().getId());
        dto.setPerformedAt(auditLog.getPerformedAt());
        return dto;
    }
}