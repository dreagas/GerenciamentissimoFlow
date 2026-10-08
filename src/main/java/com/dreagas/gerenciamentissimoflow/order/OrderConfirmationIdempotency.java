package com.dreagas.gerenciamentissimoflow.order;

import java.time.Instant;
import java.util.UUID;

import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "order_confirmation_idempotency",
		uniqueConstraints = @UniqueConstraint(name = "uq_order_confirmation_key", columnNames = { "actor_id", "idempotency_key" }),
		indexes = @Index(name = "ix_order_confirmation_expires_at", columnList = "expires_at"))
public class OrderConfirmationIdempotency extends BaseAuditableEntity {
	@Column(name = "actor_id", nullable = false)
	private UUID actorId;
	@Column(name = "idempotency_key", nullable = false, length = 128)
	private String idempotencyKey;
	@Column(name = "order_id", nullable = false)
	private UUID orderId;
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected OrderConfirmationIdempotency() { }

	public OrderConfirmationIdempotency(UUID actorId, String idempotencyKey, UUID orderId, Instant expiresAt) {
		this.actorId = actorId;
		this.idempotencyKey = idempotencyKey;
		this.orderId = orderId;
		this.expiresAt = expiresAt;
	}

	public UUID getActorId() { return actorId; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public UUID getOrderId() { return orderId; }
	public Instant getExpiresAt() { return expiresAt; }
}
