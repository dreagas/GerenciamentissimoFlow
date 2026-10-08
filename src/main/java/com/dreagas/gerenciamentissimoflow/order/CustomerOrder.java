package com.dreagas.gerenciamentissimoflow.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.dreagas.gerenciamentissimoflow.auth.User;
import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_order")
public class CustomerOrder extends BaseAuditableEntity {
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.DRAFT;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_id", nullable = false)
	private User createdBy;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
	private BigDecimal totalAmount = BigDecimal.ZERO.setScale(4);

	@Column(name = "fulfilled_at")
	private Instant fulfilledAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fulfilled_by_id")
	private User fulfilledBy;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	protected CustomerOrder() { }

	public CustomerOrder(Warehouse warehouse, User createdBy) {
		this.warehouse = Objects.requireNonNull(warehouse, "warehouse must not be null");
		this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
	}

	public OrderStatus getStatus() { return status; }
	public void requireDraft() { if (status != OrderStatus.DRAFT) throw new IllegalStateException("order is not editable"); }
	public void confirm() { requireDraft(); if (items.isEmpty()) throw new IllegalStateException("order must contain at least one item"); status = OrderStatus.CONFIRMED; }
	public void cancel() {
		if (status != OrderStatus.DRAFT && status != OrderStatus.CONFIRMED)
			throw new IllegalStateException("order cannot be cancelled from " + status);
		status = OrderStatus.CANCELLED;
	}
	public void fulfill(User actor) {
		if (status != OrderStatus.CONFIRMED) throw new IllegalStateException("only confirmed orders can be fulfilled");
		fulfilledBy = Objects.requireNonNull(actor, "actor must not be null");
		fulfilledAt = Instant.now();
		status = OrderStatus.FULFILLED;
	}
	public Instant getFulfilledAt() { return fulfilledAt; }
	public User getFulfilledBy() { return fulfilledBy; }
	public Warehouse getWarehouse() { return warehouse; }
	public User getCreatedBy() { return createdBy; }
	public BigDecimal getTotalAmount() { return totalAmount; }
	public List<OrderItem> getItems() { return List.copyOf(items); }
	public void addItem(OrderItem item) { requireDraft(); items.add(Objects.requireNonNull(item)); item.attachTo(this); recalculateTotal(); }
	public OrderItem getItem(java.util.UUID itemId) { return items.stream().filter(item -> item.getId().equals(itemId)).findFirst().orElseThrow(() -> new IllegalArgumentException("order item not found")); }
	public void removeItem(OrderItem item) { requireDraft(); if (items.remove(item)) { item.detach(); recalculateTotal(); } }
	public void recalculateItem(OrderItem item, long quantity, BigDecimal unitPrice) {
		if (!items.contains(item)) throw new IllegalArgumentException("order item does not belong to this order");
		item.update(quantity, unitPrice);
		recalculateTotal();
	}
	public void recalculateTotal() { totalAmount = items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4); }
}
