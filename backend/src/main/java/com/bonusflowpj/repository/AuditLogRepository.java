package com.bonusflowpj.repository;

import com.bonusflowpj.domain.AuditLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        select a
        from AuditLog a
        where (:entityName is null or lower(a.entityName) = lower(:entityName))
          and (:action is null or a.action = :action)
          and (:performedByUserId is null or a.performedByUserId = :performedByUserId)
          and (:startAt is null or a.performedAt >= :startAt)
          and (:endAt is null or a.performedAt <= :endAt)
        order by a.performedAt desc
        """)
    List<AuditLog> search(
        @Param("entityName") String entityName,
        @Param("action") String action,
        @Param("performedByUserId") Long performedByUserId,
        @Param("startAt") Instant startAt,
        @Param("endAt") Instant endAt
    );
}
