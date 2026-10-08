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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_balance", uniqueConstraints = @UniqueConstraint(name = "uq_inventory_product_warehouse",
		columnNames = {"product_id", "warehouse_id"}),
		check = @CheckConstraint(name = "ck_inventory_quantity_non_negative", constraint = "quantity >= 0"))
public class InventoryBalance extends BaseAuditableEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;
	@Column(nullable = false)
	private long quantity;
	@Version
	@Column(nullable = false)
	private long version;

	protected InventoryBalance() { }

	public InventoryBalance(Product product, Warehouse warehouse, long quantity) {
		this.product = Objects.requireNonNull(product, "product must not be null");
		this.warehouse = Objects.requireNonNull(warehouse, "warehouse must not be null");
		setQuantity(quantity);
	}

	public void setQuantity(long quantity) {
		if (quantity < 0) throw new IllegalArgumentException("quantity must not be negative");
		this.quantity = quantity;
	}

	public Product getProduct() { return product; }
	public Warehouse getWarehouse() { return warehouse; }
	public long getQuantity() { return quantity; }
	public long getVersion() { return version; }
}
