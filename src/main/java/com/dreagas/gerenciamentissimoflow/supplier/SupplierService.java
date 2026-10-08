package com.dreagas.gerenciamentissimoflow.supplier;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;

@Service
@Transactional
public class SupplierService {
	private final SupplierRepository repository;
	private final AuditService audit;
	@org.springframework.beans.factory.annotation.Autowired
	public SupplierService(SupplierRepository repository, AuditService audit) { this.repository = repository; this.audit = audit; }
	public SupplierService(SupplierRepository repository) { this(repository, null); }
	public Supplier create(String name, String document, String email, String phone) {
		return create(name, document, email, phone, null);
	}
	public Supplier create(String name, String document, String email, String phone, String actorEmail) {
		Supplier saved = repository.save(new Supplier(name, document, email, phone));
		if (audit != null) audit.record(actorEmail, "SUPPLIER_CREATED", "SUPPLIER", saved.getId());
		return saved;
	}
	public Supplier update(UUID id, String name, String document, String email, String phone) {
		return update(id, name, document, email, phone, null);
	}
	public Supplier update(UUID id, String name, String document, String email, String phone, String actorEmail) {
		Supplier supplier = get(id);
		supplier.update(name, document, email, phone);
		if (audit != null) audit.record(actorEmail, "SUPPLIER_UPDATED", "SUPPLIER", supplier.getId());
		return supplier;
	}
	@Transactional(readOnly = true)
	public Supplier get(UUID id) { return repository.findById(id).orElseThrow(SupplierNotFoundException::new); }
	@Transactional(readOnly = true)
	public Page<Supplier> search(String query, Boolean active, Pageable pageable) {
		String term = query == null ? "" : query.trim();
		if (term.isEmpty()) return active == null ? repository.findAll(pageable) : repository.findAllByActive(active, pageable);
		return active == null
				? repository.findAllByNameContainingIgnoreCaseOrDocumentContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, term, pageable)
				: repository.findAllByNameContainingIgnoreCaseOrDocumentContainingIgnoreCaseOrEmailContainingIgnoreCaseAndActive(term, term, term, active, pageable);
	}
	public Supplier deactivate(UUID id) { return deactivate(id, null); }
	public Supplier deactivate(UUID id, String actorEmail) { Supplier supplier = get(id); supplier.deactivate(); if (audit != null) audit.record(actorEmail, "SUPPLIER_DEACTIVATED", "SUPPLIER", supplier.getId()); return supplier; }
}
