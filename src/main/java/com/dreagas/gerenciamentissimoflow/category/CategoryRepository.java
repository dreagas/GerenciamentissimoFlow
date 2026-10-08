package com.dreagas.gerenciamentissimoflow.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryRepository extends JpaRepository<Category, java.util.UUID> {
	boolean existsByNameIgnoreCase(String name);
	Page<Category> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
	Page<Category> findAllByActive(boolean active, Pageable pageable);
	Page<Category> findAllByNameContainingIgnoreCaseAndActive(String name, boolean active, Pageable pageable);
}
