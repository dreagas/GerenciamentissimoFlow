package com.dreagas.gerenciamentissimoflow.product;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {
	long countByActiveTrue();
	boolean existsBySku(String sku);
	java.util.Optional<Product> findBySku(String sku);
	Page<Product> findAllByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku, Pageable pageable);
	Page<Product> findAllByCategory_Id(UUID categoryId, Pageable pageable);
	@Query("select p from Product p where p.category.id = :categoryId and p.active = :active and "
			+ "(lower(p.name) like lower(concat('%', :term, '%')) or lower(p.sku) like lower(concat('%', :term, '%')))")
	Page<Product> searchByNameSkuCategoryAndActive(@Param("term") String term, @Param("categoryId") UUID categoryId,
			@Param("active") Boolean active, Pageable pageable);
	@Query("select p from Product p where p.category.id = :categoryId and "
			+ "(lower(p.name) like lower(concat('%', :term, '%')) or lower(p.sku) like lower(concat('%', :term, '%')))")
	Page<Product> searchByNameSkuAndCategory(@Param("term") String term, @Param("categoryId") UUID categoryId,
			Pageable pageable);
	Page<Product> findAllByActive(boolean active, Pageable pageable);
	Page<Product> findAllByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseAndActive(String name, String sku,
			boolean active, Pageable pageable);
	Page<Product> findAllByCategory_IdAndActive(UUID categoryId, boolean active, Pageable pageable);
}
