package com.dreagas.gerenciamentissimoflow.product;

import java.util.Locale;
import java.util.UUID;

import com.dreagas.gerenciamentissimoflow.category.Category;
import com.dreagas.gerenciamentissimoflow.category.CategoryNotFoundException;
import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {
	private final ProductRepository products;
	private final CategoryRepository categories;
	private final AuditService audit;

	@org.springframework.beans.factory.annotation.Autowired
	public ProductService(ProductRepository products, CategoryRepository categories, AuditService audit) {
		this.products = products;
		this.categories = categories;
		this.audit = audit;
	}

	public ProductService(ProductRepository products, CategoryRepository categories) { this(products, categories, null); }

	public Product create(ProductData data) {
		return create(data, null);
	}

	public Product create(ProductData data, String actorEmail) {
		String sku = normalizeSku(data.sku());
		ensureSkuAvailable(sku, null);
		Product product = new Product(sku, data.name(), data.description(), resolveCategory(data.categoryId()),
				data.referencePrice(), data.minimumStock());
		Product saved = products.save(product);
		if (audit != null) audit.record(actorEmail, "PRODUCT_CREATED", "PRODUCT", saved.getId());
		return saved;
	}

	public Product update(UUID id, ProductData data) {
		return update(id, data, null);
	}

	public Product update(UUID id, ProductData data, String actorEmail) {
		Product product = get(id);
		String sku = normalizeSku(data.sku());
		ensureSkuAvailable(sku, id);
		product.update(sku, data.name(), data.description(), resolveCategory(data.categoryId()),
				data.referencePrice(), data.minimumStock());
		if (audit != null) audit.record(actorEmail, "PRODUCT_UPDATED", "PRODUCT", product.getId());
		return product;
	}

	@Transactional(readOnly = true)
	public Product get(UUID id) {
		return products.findById(id).orElseThrow(ProductNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public Page<Product> search(String query, UUID categoryId, Boolean active, Pageable pageable) {
		if (categoryId != null && !categories.existsById(categoryId)) throw new CategoryNotFoundException();
		String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		if (!term.isEmpty() && categoryId != null && active != null)
			return products.searchByNameSkuCategoryAndActive(term, categoryId, active, pageable);
		if (!term.isEmpty() && categoryId != null)
			return products.searchByNameSkuAndCategory(term, categoryId, pageable);
		if (term.isEmpty() && categoryId != null) {
			if (active != null) return products.findAllByCategory_IdAndActive(categoryId, active, pageable);
			return products.findAllByCategory_Id(categoryId, pageable);
		}
		if (term.isEmpty() && active != null) return products.findAllByActive(active, pageable);
		if (!term.isEmpty() && active != null)
			return products.findAllByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseAndActive(term, term, active, pageable);
		return products.findAllByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(term, term, pageable);
	}

	public Product deactivate(UUID id) {
		return deactivate(id, null);
	}

	public Product deactivate(UUID id, String actorEmail) {
		Product product = get(id);
		product.deactivate();
		if (audit != null) audit.record(actorEmail, "PRODUCT_DEACTIVATED", "PRODUCT", product.getId());
		return product;
	}

	private Category resolveCategory(UUID id) {
		if (id == null) return null;
		Category category = categories.findById(id).orElseThrow(CategoryNotFoundException::new);
		if (!category.isActive()) throw new CategoryNotFoundException();
		return category;
	}

	private void ensureSkuAvailable(String sku, UUID currentId) {
		products.findBySku(sku).filter(existing -> !existing.getId().equals(currentId))
				.ifPresent(existing -> { throw new ProductConflictException(); });
	}

	private static String normalizeSku(String sku) { return sku.trim().toUpperCase(Locale.ROOT); }
	public record ProductData(String sku, String name, String description, UUID categoryId,
			java.math.BigDecimal referencePrice, long minimumStock) { }
}
