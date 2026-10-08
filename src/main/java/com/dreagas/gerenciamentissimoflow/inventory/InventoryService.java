package com.dreagas.gerenciamentissimoflow.inventory;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;

@Service
public class InventoryService {
	private final InventoryBalanceRepository balances;
	private final InventoryMovementRepository movements;
	private final ProductRepository products;
	private final WarehouseRepository warehouses;
	private final AuditService audit;

	@org.springframework.beans.factory.annotation.Autowired
	public InventoryService(InventoryBalanceRepository balances, InventoryMovementRepository movements,
			ProductRepository products, WarehouseRepository warehouses, AuditService audit) {
		this.balances = balances; this.movements = movements; this.products = products; this.warehouses = warehouses; this.audit = audit;
	}

	public InventoryService(InventoryBalanceRepository balances, InventoryMovementRepository movements,
			ProductRepository products, WarehouseRepository warehouses) {
		this(balances, movements, products, warehouses, null);
	}

	@Transactional
	public InventoryBalance receive(UUID productId, UUID warehouseId, long quantity, String reason) {
		return receive(productId, warehouseId, quantity, reason, null);
	}

	@Transactional
	public InventoryBalance receive(UUID productId, UUID warehouseId, long quantity, String reason, String actorEmail) {
		InventoryBalance balance = change(productId, warehouseId, quantity, "RECEIPT", reason);
		if (audit != null) audit.record(actorEmail, "STOCK_RECEIVED", "INVENTORY_BALANCE", balance.getId());
		return balance;
	}

	@Transactional
	public InventoryBalance adjust(UUID productId, UUID warehouseId, long targetQuantity, String reason) {
		return adjust(productId, warehouseId, targetQuantity, reason, null);
	}

	@Transactional
	public InventoryBalance adjust(UUID productId, UUID warehouseId, long targetQuantity, String reason, String actorEmail) {
		if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required");
		if (targetQuantity < 0) throw new IllegalArgumentException("target quantity must not be negative");
		Product product = products.findById(productId).filter(Product::isActive)
				.orElseThrow(() -> new IllegalArgumentException("Active product not found"));
		Warehouse warehouse = warehouses.findById(warehouseId).filter(Warehouse::isActive)
				.orElseThrow(() -> new IllegalArgumentException("Active warehouse not found"));
		InventoryBalance balance = balances.lockByProductAndWarehouse(productId, warehouseId).orElseGet(() ->
				balances.saveAndFlush(new InventoryBalance(product, warehouse, 0)));
		long delta = Math.subtractExact(targetQuantity, balance.getQuantity());
		if (delta == 0) throw new IllegalArgumentException("adjustment must change the balance");
		balance.setQuantity(targetQuantity);
		InventoryBalance saved = balances.saveAndFlush(balance);
		movements.save(new InventoryMovement(product, warehouse, "ADJUSTMENT", Math.abs(delta), reason.trim()));
		if (audit != null) audit.record(actorEmail, "STOCK_ADJUSTED", "INVENTORY_BALANCE", saved.getId());
		return saved;
	}

	@Transactional
	public void consumeForOrder(java.util.UUID warehouseId, java.util.UUID orderId,
			java.util.List<com.dreagas.gerenciamentissimoflow.order.OrderItem> items) {
		var productIds = items.stream().map(item -> item.getProduct().getId()).distinct().sorted().toList();
		if (items.stream().map(item -> item.getProduct().getId()).distinct().count() != items.size())
			throw new IllegalArgumentException("Order cannot contain duplicate products");
		var balancesByProduct = balances.lockAllForProducts(productIds, warehouseId).stream()
				.collect(java.util.stream.Collectors.toMap(balance -> balance.getProduct().getId(), balance -> balance));
		for (var item : items) {
			if (!item.getProduct().isActive()) throw new IllegalArgumentException("Inactive product in order");
			InventoryBalance balance = balancesByProduct.get(item.getProduct().getId());
			if (balance == null || balance.getQuantity() < item.getQuantity()) throw new IllegalArgumentException("Insufficient stock");
		}
		for (var item : items) {
			InventoryBalance balance = balancesByProduct.get(item.getProduct().getId());
			balance.setQuantity(balance.getQuantity() - item.getQuantity());
			balances.save(balance);
			movements.save(new InventoryMovement(item.getProduct(), balance.getWarehouse(), "ORDER_OUT",
					item.getQuantity(), "Order confirmation", orderId));
		}
	}

	@Transactional
	public void releaseForOrder(java.util.UUID warehouseId, java.util.UUID orderId,
			java.util.List<com.dreagas.gerenciamentissimoflow.order.OrderItem> items) {
		var productIds = items.stream().map(item -> item.getProduct().getId()).distinct().sorted().toList();
		var balancesByProduct = balances.lockAllForProducts(productIds, warehouseId).stream()
				.collect(java.util.stream.Collectors.toMap(balance -> balance.getProduct().getId(), balance -> balance));
		for (var item : items) {
			InventoryBalance balance = balancesByProduct.get(item.getProduct().getId());
			if (balance == null) throw new IllegalStateException("inventory balance missing for confirmed order");
			balance.setQuantity(Math.addExact(balance.getQuantity(), item.getQuantity()));
			balances.save(balance);
			movements.save(new InventoryMovement(item.getProduct(), balance.getWarehouse(), "ORDER_CANCEL_IN",
					item.getQuantity(), "Order cancellation", orderId));
		}
	}

	private InventoryBalance change(UUID productId, UUID warehouseId, long quantity, String type, String reason) {
		if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
		Product product = products.findById(productId).filter(Product::isActive)
				.orElseThrow(() -> new IllegalArgumentException("Active product not found"));
		Warehouse warehouse = warehouses.findById(warehouseId).filter(Warehouse::isActive)
				.orElseThrow(() -> new IllegalArgumentException("Active warehouse not found"));
		InventoryBalance balance = balances.lockByProductAndWarehouse(productId, warehouseId).orElseGet(() ->
				balances.saveAndFlush(new InventoryBalance(product, warehouse, 0)));
		balance.setQuantity(Math.addExact(balance.getQuantity(), quantity));
		InventoryBalance saved = balances.saveAndFlush(balance);
		movements.save(new InventoryMovement(product, warehouse, type, quantity, reason));
		return saved;
	}
}
