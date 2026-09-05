package com.sellam.store.support.repositories;

import com.sellam.store.support.models.SupportAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportAuditLogRepository extends JpaRepository<SupportAuditLogEntity, UUID> {
    List<SupportAuditLogEntity> findByTargetUserIdOrderByTimestampDesc(UUID targetUserId);
}
