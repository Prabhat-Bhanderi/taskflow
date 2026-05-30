package com.taskflow.audit;

import com.taskflow.common.enums.AuditAction;
import com.taskflow.common.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // fetch all logs for a specific entity
    List<AuditLog> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    // fetch paginated logs with filters
    Page<AuditLog> findByEntityType(EntityType entityType, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByPerformedBy_Id(Long userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndAction(EntityType entityType, AuditAction action, Pageable pageable);
}
