package com.dreagas.gerenciamentissimoflow.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderConfirmationIdempotencyRepository extends JpaRepository<OrderConfirmationIdempotency, UUID> {
	Optional<OrderConfirmationIdempotency> findByActorIdAndIdempotencyKey(UUID actorId, String idempotencyKey);
}
