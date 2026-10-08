package com.dreagas.gerenciamentissimoflow.order;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.dreagas.gerenciamentissimoflow.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_item")
public class OrderItem {
	@jakarta.persistence.Id
	@jakarta.persistence.Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private long quantity;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
	private BigDecimal unitPrice;

	@Column(name = "line_total", nullable = false, precision = 19, scale = 4)
	private BigDecimal lineTotal;

	protected OrderItem() { }

	@jakarta.persistence.PrePersist
	private void assignId() { if (id == null) id = UUID.randomUUID(); }

	public OrderItem(Product product, long quantity, BigDecimal unitPrice) {
		this.product = Objects.requireNonNull(product, "product must not be null");
		if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
		this.quantity = quantity;
		this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
		if (unitPrice.signum() < 0 || unitPrice.scale() > 4) throw new IllegalArgumentException("unitPrice must be non-negative with at most four decimal places");
		this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	void attachTo(CustomerOrder order) { this.order = order; }
	void detach() { this.order = null; }
	public UUID getId() { return id; }
	void update(long quantity, BigDecimal unitPrice) {
		if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
		Objects.requireNonNull(unitPrice, "unitPrice must not be null");
		if (unitPrice.signum() < 0 || unitPrice.scale() > 4 || unitPrice.precision() > 19) throw new IllegalArgumentException("unitPrice must be non-negative with at most four decimal places");
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
	public Product getProduct() { return product; }
	public CustomerOrder getOrder() { return order; }
	public long getQuantity() { return quantity; }
	public BigDecimal getUnitPrice() { return unitPrice; }
	public BigDecimal getLineTotal() { return lineTotal; }
}
