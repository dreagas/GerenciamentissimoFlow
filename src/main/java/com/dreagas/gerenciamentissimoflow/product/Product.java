package com.dreagas.gerenciamentissimoflow.product;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

import com.dreagas.gerenciamentissimoflow.category.Category;
import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "product")
public class Product extends BaseAuditableEntity {

	@Column(nullable = false, unique = true, length = 80)
	private String sku;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(name = "reference_price", nullable = false, precision = 19, scale = 4)
	private BigDecimal referencePrice;

	@Column(name = "minimum_stock", nullable = false)
	private long minimumStock;

	@Column(nullable = false)
	private boolean active = true;

	protected Product() {
	}

	public Product(String sku, String name, String description, Category category, BigDecimal referencePrice,
			long minimumStock) {
		update(sku, name, description, category, referencePrice, minimumStock);
	}

	public void update(String sku, String name, String description, Category category, BigDecimal referencePrice,
			long minimumStock) {
		this.sku = normalizeSku(sku);
		this.name = requireText(name, "name");
		this.description = description == null || description.isBlank() ? null : description.trim();
		this.category = category;
		this.referencePrice = Objects.requireNonNull(referencePrice, "referencePrice must not be null");
		if (referencePrice.signum() < 0 || referencePrice.scale() > 4 || referencePrice.precision() > 19) {
			throw new IllegalArgumentException("referencePrice must be non-negative with at most 4 decimal places");
		}
		if (minimumStock < 0) {
			throw new IllegalArgumentException("minimumStock must not be negative");
		}
		this.minimumStock = minimumStock;
	}

	public void deactivate() {
		active = false;
	}

	@PrePersist
	@PreUpdate
	private void normalizeFields() {
		sku = normalizeSku(sku);
		name = requireText(name, "name");
	}

	public String getSku() { return sku; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public Category getCategory() { return category; }
	public BigDecimal getReferencePrice() { return referencePrice; }
	public long getMinimumStock() { return minimumStock; }
	public boolean isActive() { return active; }

	private static String normalizeSku(String sku) {
		return requireText(sku, "sku").toUpperCase(Locale.ROOT);
	}

	private static String requireText(String value, String field) {
		String normalized = Objects.requireNonNull(value, field + " must not be null").trim();
		if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
		return normalized;
	}
}
