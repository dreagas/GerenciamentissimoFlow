package com.dreagas.gerenciamentissimoflow.inventory;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {
	@Query("select count(b) from InventoryBalance b where b.quantity = 0")
	long countOutOfStockBalances();
	@Query("select count(b) from InventoryBalance b join b.product p where b.quantity <= p.minimumStock")
	long countLowStockBalances();
	Optional<InventoryBalance> findByProduct_IdAndWarehouse_Id(UUID productId, UUID warehouseId);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from InventoryBalance b where b.product.id = :productId and b.warehouse.id = :warehouseId")
	Optional<InventoryBalance> lockByProductAndWarehouse(@Param("productId") UUID productId,
			@Param("warehouseId") UUID warehouseId);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from InventoryBalance b join fetch b.product where b.product.id in :productIds and b.warehouse.id = :warehouseId order by b.product.id")
	java.util.List<InventoryBalance> lockAllForProducts(@Param("productIds") java.util.Collection<UUID> productIds,
			@Param("warehouseId") UUID warehouseId);
	@Query("select b from InventoryBalance b join fetch b.product p join fetch b.warehouse w "
			+ "left join p.category c where (:productId is null or p.id = :productId) "
			+ "and (:warehouseId is null or w.id = :warehouseId) "
			+ "and (:categoryId is null or c.id = :categoryId) "
			+ "and (:lowStock = false or b.quantity <= p.minimumStock) "
			+ "and (:outOfStock = false or b.quantity = 0)")
	Page<InventoryBalance> search(@Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId,
			@Param("categoryId") UUID categoryId, @Param("lowStock") boolean lowStock,
			@Param("outOfStock") boolean outOfStock, Pageable pageable);
}
