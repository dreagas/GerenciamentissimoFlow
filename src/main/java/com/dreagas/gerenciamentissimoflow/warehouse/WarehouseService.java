package com.dreagas.gerenciamentissimoflow.warehouse;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;

@Service
@Transactional
public class WarehouseService {
	private final WarehouseRepository repository;
	private final AuditService audit;

	@org.springframework.beans.factory.annotation.Autowired
	public WarehouseService(WarehouseRepository repository, AuditService audit) { this.repository = repository; this.audit = audit; }
	public WarehouseService(WarehouseRepository repository) { this(repository, null); }

	public Warehouse create(String code, String name, String location) {
		return create(code, name, location, null);
	}
	public Warehouse create(String code, String name, String location, String actorEmail) {
		String normalized = code == null ? null : code.trim();
		if (normalized != null && repository.existsByCodeIgnoreCase(normalized)) throw new WarehouseConflictException();
		try {
			Warehouse saved = repository.saveAndFlush(new Warehouse(code, name, location));
			if (audit != null) audit.record(actorEmail, "WAREHOUSE_CREATED", "WAREHOUSE", saved.getId());
			return saved;
		} catch (DataIntegrityViolationException ex) {
			throw new WarehouseConflictException();
		}
	}

	@Transactional(readOnly = true)
	public Warehouse get(UUID id) { return repository.findById(id).orElseThrow(() -> new WarehouseNotFoundException(id)); }

	@Transactional(readOnly = true)
	public Page<Warehouse> list(Boolean active, String query, Pageable pageable) {
		if (active == null && (query == null || query.isBlank())) return repository.findAll(pageable);
		if (active != null && (query == null || query.isBlank())) return repository.findAllByActive(active, pageable);
		String term = query == null ? "" : query.trim();
		if (active == null) return repository.findAllByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
				term, term, term, pageable);
		return repository.searchByActiveAndTerm(active, term, pageable);
	}

	public Warehouse update(UUID id, String code, String name, String location) {
		return update(id, code, name, location, null);
	}
	public Warehouse update(UUID id, String code, String name, String location, String actorEmail) {
		Warehouse warehouse = get(id);
		String normalized = code == null ? null : code.trim();
		if (normalized != null && !warehouse.getCode().equalsIgnoreCase(normalized)
				&& repository.existsByCodeIgnoreCase(normalized)) throw new WarehouseConflictException();
		warehouse.update(code, name, location);
		try { Warehouse saved = repository.saveAndFlush(warehouse); if (audit != null) audit.record(actorEmail, "WAREHOUSE_UPDATED", "WAREHOUSE", saved.getId()); return saved; }
		catch (DataIntegrityViolationException ex) { throw new WarehouseConflictException(); }
	}

	public Warehouse deactivate(UUID id) {
		return deactivate(id, null);
	}
	public Warehouse deactivate(UUID id, String actorEmail) {
		Warehouse warehouse = get(id);
		warehouse.deactivate();
		Warehouse saved = repository.save(warehouse);
		if (audit != null) audit.record(actorEmail, "WAREHOUSE_DEACTIVATED", "WAREHOUSE", saved.getId());
		return saved;
	}
}
