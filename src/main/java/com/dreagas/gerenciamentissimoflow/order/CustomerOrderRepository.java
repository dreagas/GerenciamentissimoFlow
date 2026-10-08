package com.dreagas.gerenciamentissimoflow.order;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
	long countByStatus(OrderStatus status);
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select o from CustomerOrder o where o.id = :id")
	java.util.Optional<CustomerOrder> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
	Page<CustomerOrder> findAllByCreatedBy_Id(UUID userId, Pageable pageable);
	@org.springframework.data.jpa.repository.Query("select distinct o from CustomerOrder o join fetch o.warehouse join fetch o.createdBy left join fetch o.items i left join fetch i.product where o.id = :id")
	java.util.Optional<CustomerOrder> findDetailedById(@org.springframework.data.repository.query.Param("id") UUID id);
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select distinct o from CustomerOrder o join fetch o.warehouse join fetch o.createdBy left join fetch o.items i left join fetch i.product where o.id = :id")
	java.util.Optional<CustomerOrder> lockDetailedById(@org.springframework.data.repository.query.Param("id") UUID id);
	@org.springframework.data.jpa.repository.Query(value = "select distinct o from CustomerOrder o join fetch o.warehouse join fetch o.createdBy left join fetch o.items i left join fetch i.product",
		countQuery = "select count(o) from CustomerOrder o")
	Page<CustomerOrder> findDetailedAll(Pageable pageable);
	@org.springframework.data.jpa.repository.Query(value = "select distinct o from CustomerOrder o join fetch o.warehouse join fetch o.createdBy left join fetch o.items i left join fetch i.product where o.createdBy.id = :userId",
		countQuery = "select count(o) from CustomerOrder o where o.createdBy.id = :userId")
	Page<CustomerOrder> findDetailedByCreator(@org.springframework.data.repository.query.Param("userId") UUID userId, Pageable pageable);
}
