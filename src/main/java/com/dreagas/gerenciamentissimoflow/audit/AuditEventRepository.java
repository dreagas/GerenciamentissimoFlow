package com.dreagas.gerenciamentissimoflow.audit;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
	@Query("select e from AuditEvent e where (:actorId is null or e.actorId = :actorId) "
			+ "and (:action is null or e.action = :action) and (:entityType is null or e.entityType = :entityType) "
			+ "order by e.occurredAt desc")
	Page<AuditEvent> search(@Param("actorId") UUID actorId, @Param("action") String action,
			@Param("entityType") String entityType, Pageable pageable);
}
