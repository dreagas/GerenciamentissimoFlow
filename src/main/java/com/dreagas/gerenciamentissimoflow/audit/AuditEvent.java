package com.dreagas.gerenciamentissimoflow.audit;

import java.time.Instant;
import java.util.UUID;

import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_event", indexes = {
		@Index(name = "ix_audit_event_occurred_at", columnList = "occurred_at"),
		@Index(name = "ix_audit_event_actor", columnList = "actor_id")
})
public class AuditEvent extends BaseAuditableEntity {
	@Column(name = "actor_id")
	private UUID actorId;

	@Column(name = "actor_email", length = 320)
	private String actorEmail;

	@Column(nullable = false, length = 80)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 80)
	private String entityType;

	@Column(name = "entity_id")
	private UUID entityId;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	protected AuditEvent() { }

	AuditEvent(UUID actorId, String actorEmail, String action, String entityType, UUID entityId) {
		this.actorId = actorId;
		this.actorEmail = actorEmail;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.occurredAt = Instant.now();
	}

	public UUID getActorId() { return actorId; }
	public String getActorEmail() { return actorEmail; }
	public String getAction() { return action; }
	public String getEntityType() { return entityType; }
	public UUID getEntityId() { return entityId; }
	public Instant getOccurredAt() { return occurredAt; }
}
