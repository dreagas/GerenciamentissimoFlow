package com.dreagas.gerenciamentissimoflow.inventory;

import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
	java.util.List<InventoryMovement> findTop10ByOrderByCreatedAtDesc();
	@Query("select m from InventoryMovement m join fetch m.product p join fetch m.warehouse w "
			+ "where (:productId is null or p.id = :productId) and (:warehouseId is null or w.id = :warehouseId) "
			+ "and (:type is null or m.type = :type) "
			+ "and m.createdAt >= :from and m.createdAt <= :to")
	Page<InventoryMovement> search(@Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId,
			@Param("type") String type, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
	long countByReferenceIdAndType(UUID referenceId, String type);
}
