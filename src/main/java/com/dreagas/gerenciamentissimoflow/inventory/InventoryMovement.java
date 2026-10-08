package com.dreagas.gerenciamentissimoflow.inventory;

import java.util.Objects;

import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement extends BaseAuditableEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;
	@Column(nullable = false, length = 30)
	private String type;
	@Column(nullable = false)
	private long quantity;
	@Column(length = 500)
	private String reason;
	@Column(name = "reference_id")
	private java.util.UUID referenceId;

	protected InventoryMovement() { }

	public InventoryMovement(Product product, Warehouse warehouse, String type, long quantity, String reason) {
		this(product, warehouse, type, quantity, reason, null);
	}

	public InventoryMovement(Product product, Warehouse warehouse, String type, long quantity, String reason, java.util.UUID referenceId) {
		this.product = Objects.requireNonNull(product);
		this.warehouse = Objects.requireNonNull(warehouse);
		this.type = Objects.requireNonNull(type);
		if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
		this.quantity = quantity;
		this.reason = reason == null || reason.isBlank() ? null : reason.trim();
		this.referenceId = referenceId;
	}

	public Product getProduct() { return product; }
	public Warehouse getWarehouse() { return warehouse; }
	public String getType() { return type; }
	public long getQuantity() { return quantity; }
	public String getReason() { return reason; }
	public java.util.UUID getReferenceId() { return referenceId; }
}
