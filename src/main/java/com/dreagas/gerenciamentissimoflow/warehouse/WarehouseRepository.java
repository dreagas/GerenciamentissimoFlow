package com.dreagas.gerenciamentissimoflow.warehouse;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
	boolean existsByCodeIgnoreCase(String code);
	Page<Warehouse> findAllByActive(boolean active, Pageable pageable);
	Page<Warehouse> findAllByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
			String code, String name, String location, Pageable pageable);
	@org.springframework.data.jpa.repository.Query("select w from Warehouse w where w.active = :active and "
			+ "(lower(w.code) like lower(concat('%', :term, '%')) or lower(w.name) like lower(concat('%', :term, '%')) "
			+ "or lower(w.location) like lower(concat('%', :term, '%'))) ")
	Page<Warehouse> searchByActiveAndTerm(boolean active, String term, Pageable pageable);
}
