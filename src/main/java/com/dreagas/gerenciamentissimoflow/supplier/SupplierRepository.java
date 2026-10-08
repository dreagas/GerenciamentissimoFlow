package com.dreagas.gerenciamentissimoflow.supplier;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
	Page<Supplier> findAllByNameContainingIgnoreCaseOrDocumentContainingIgnoreCaseOrEmailContainingIgnoreCase(
			String name, String document, String email, Pageable pageable);
	Page<Supplier> findAllByActive(boolean active, Pageable pageable);
	Page<Supplier> findAllByNameContainingIgnoreCaseOrDocumentContainingIgnoreCaseOrEmailContainingIgnoreCaseAndActive(
			String name, String document, String email, boolean active, Pageable pageable);
}
