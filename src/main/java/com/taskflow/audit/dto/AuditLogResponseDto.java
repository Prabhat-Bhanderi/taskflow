package com.taskflow.audit.dto;

import com.taskflow.common.enums.AuditAction;
import com.taskflow.common.enums.EntityType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuditLogResponseDto {

    private Long id;
    private EntityType entityType;
    private Long entityId;
    private AuditAction action;
    private String changedFields;
    private Long performedBy;
    private LocalDateTime performedAt;
}