package com.dreagas.gerenciamentissimoflow.category;

import java.util.UUID;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;

@Service
@Transactional
public class CategoryService {

	private final CategoryRepository repository;
	private final AuditService audit;

	@org.springframework.beans.factory.annotation.Autowired
	public CategoryService(CategoryRepository repository, AuditService audit) {
		this.repository = repository;
		this.audit = audit;
	}

	public CategoryService(CategoryRepository repository) { this(repository, null); }

	public Category create(String name) {
		return create(name, null);
	}

	public Category create(String name, String actorEmail) {
		Category category = new Category(name);
		if (repository.existsByNameIgnoreCase(category.getName())) {
			throw new CategoryConflictException();
		}
		Category saved = repository.save(category);
		if (audit != null) audit.record(actorEmail, "CATEGORY_CREATED", "CATEGORY", saved.getId());
		return saved;
	}

	public boolean exists(UUID id) {
		return repository.existsById(id);
	}

	@Transactional(readOnly = true)
	public Page<Category> list(Pageable pageable) {
		return repository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Page<Category> search(String query, Boolean active, Pageable pageable) {
		if (query == null || query.isBlank()) {
			return active == null ? repository.findAll(pageable) : repository.findAllByActive(active, pageable);
		}
		String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
		return active == null ? repository.findAllByNameContainingIgnoreCase(normalizedQuery, pageable)
				: repository.findAllByNameContainingIgnoreCaseAndActive(normalizedQuery, active, pageable);
	}

	@Transactional(readOnly = true)
	public Category get(UUID id) {
		return repository.findById(id).orElseThrow(CategoryNotFoundException::new);
	}

	public Category rename(UUID id, String name) {
		return rename(id, name, null);
	}

	public Category rename(UUID id, String name, String actorEmail) {
		Category category = get(id);
		Category renamed = new Category(name);
		if (!category.getName().equalsIgnoreCase(renamed.getName())
				&& repository.existsByNameIgnoreCase(renamed.getName())) {
			throw new CategoryConflictException();
		}
		category.rename(name);
		if (audit != null) audit.record(actorEmail, "CATEGORY_UPDATED", "CATEGORY", category.getId());
		return category;
	}

	public Category deactivate(UUID id) {
		return deactivate(id, null);
	}

	public Category deactivate(UUID id, String actorEmail) {
		Category category = get(id);
		category.deactivate();
		if (audit != null) audit.record(actorEmail, "CATEGORY_DEACTIVATED", "CATEGORY", category.getId());
		return category;
	}
}
