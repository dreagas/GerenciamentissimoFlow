package com.dreagas.gerenciamentissimoflow.order;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dreagas.gerenciamentissimoflow.auth.User;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryService;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class OrderService {
	private final CustomerOrderRepository orders;
	private final UserRepository users;
	private final WarehouseRepository warehouses;
	private final ProductRepository products;
	private final InventoryService inventory;
	private final OrderConfirmationIdempotencyRepository idempotency;
	private final AuditService audit;

	@org.springframework.beans.factory.annotation.Autowired
	public OrderService(CustomerOrderRepository orders, UserRepository users, WarehouseRepository warehouses, ProductRepository products, InventoryService inventory, OrderConfirmationIdempotencyRepository idempotency, AuditService audit) {
		this.orders = orders; this.users = users; this.warehouses = warehouses; this.products = products; this.inventory = inventory; this.idempotency = idempotency; this.audit = audit;
	}

	public OrderService(CustomerOrderRepository orders, UserRepository users, WarehouseRepository warehouses,
			ProductRepository products, InventoryService inventory, OrderConfirmationIdempotencyRepository idempotency) {
		this(orders, users, warehouses, products, inventory, idempotency, null);
	}

	@Transactional
	public CustomerOrder create(UUID warehouseId, String email) {
		User user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("authenticated user does not exist"));
		if (!user.isEnabled()) throw new IllegalArgumentException("disabled user cannot create orders");
		Warehouse warehouse = warehouses.findById(warehouseId).filter(Warehouse::isActive)
				.orElseThrow(() -> new IllegalArgumentException("active warehouse not found"));
		return orders.save(new CustomerOrder(warehouse, user));
	}

	@Transactional(readOnly = true)
	public CustomerOrder get(UUID id, String email, boolean canReadAll) {
		CustomerOrder order = orders.findDetailedById(id).orElseThrow(() -> new IllegalArgumentException("order not found"));
		if (!canReadAll && !order.getCreatedBy().getEmail().equals(email)) throw new org.springframework.security.access.AccessDeniedException("order belongs to another user");
		return order;
	}

	@Transactional(readOnly = true)
	public Page<CustomerOrder> list(String email, boolean canReadAll, Pageable pageable) {
		if (canReadAll) return orders.findDetailedAll(pageable);
		User user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("authenticated user does not exist"));
		return orders.findDetailedByCreator(user.getId(), pageable);
	}

	@Transactional
	public CustomerOrder addItem(UUID orderId, String email, boolean canManageAll, UUID productId, long quantity, BigDecimal unitPrice) {
		CustomerOrder order = editableOrder(orderId, email, canManageAll);
		Product product = products.findById(productId).filter(Product::isActive).orElseThrow(() -> new IllegalArgumentException("active product not found"));
		order.addItem(new OrderItem(product, quantity, unitPrice));
		order.recalculateTotal();
		return order;
	}

	@Transactional
	public CustomerOrder updateItem(UUID orderId, UUID itemId, String email, boolean canManageAll, long quantity, BigDecimal unitPrice) {
		CustomerOrder order = editableOrder(orderId, email, canManageAll);
		order.recalculateItem(order.getItem(itemId), quantity, unitPrice);
		return order;
	}

	@Transactional
	public CustomerOrder removeItem(UUID orderId, UUID itemId, String email, boolean canManageAll) {
		CustomerOrder order = editableOrder(orderId, email, canManageAll);
		order.removeItem(order.getItem(itemId));
		return order;
	}

	@Transactional
	public CustomerOrder confirm(UUID orderId, String email, boolean canManageAll) {
		CustomerOrder order = editableOrder(orderId, email, canManageAll);
		if (!order.getWarehouse().isActive()) throw new IllegalArgumentException("Inactive warehouse");
		inventory.consumeForOrder(order.getWarehouse().getId(), order.getId(), order.getItems());
		order.confirm();
		if (audit != null) audit.record(email, "ORDER_CONFIRMED", "ORDER", order.getId());
		return order;
	}

	@Transactional
	public CustomerOrder confirm(UUID orderId, String email, boolean canManageAll, String key) {
		if (key == null || key.isBlank() || key.length() > 128) throw new IllegalArgumentException("Idempotency-Key must contain 1 to 128 characters");
		User actor = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("authenticated user does not exist"));
		CustomerOrder order = orders.lockDetailedById(orderId).orElseThrow(() -> new IllegalArgumentException("order not found"));
		if (!canManageAll && !order.getCreatedBy().getEmail().equals(email)) throw new org.springframework.security.access.AccessDeniedException("order belongs to another user");
		var previous = idempotency.findByActorIdAndIdempotencyKey(actor.getId(), key);
		if (previous.isPresent()) {
			OrderConfirmationIdempotency record = previous.get();
			if (!record.getOrderId().equals(orderId)) throw new IdempotencyConflictException();
			return order;
		}
		order.requireDraft();
		if (!order.getWarehouse().isActive()) throw new IllegalArgumentException("Inactive warehouse");
		inventory.consumeForOrder(order.getWarehouse().getId(), order.getId(), order.getItems());
		order.confirm();
		idempotency.saveAndFlush(new OrderConfirmationIdempotency(actor.getId(), key, orderId,
				Instant.now().plus(30, ChronoUnit.DAYS)));
		if (audit != null) audit.record(email, "ORDER_CONFIRMED", "ORDER", order.getId());
		return order;
	}

	@Transactional
	public CustomerOrder cancel(UUID orderId, String email, boolean canManageAll) {
		CustomerOrder order = orders.lockDetailedById(orderId).orElseThrow(() -> new IllegalArgumentException("order not found"));
		if (!canManageAll && !order.getCreatedBy().getEmail().equals(email))
			throw new org.springframework.security.access.AccessDeniedException("order belongs to another user");
		if (order.getStatus() == OrderStatus.CANCELLED) return order;
		if (order.getStatus() == OrderStatus.CONFIRMED)
			inventory.releaseForOrder(order.getWarehouse().getId(), order.getId(), order.getItems());
		order.cancel();
		if (audit != null) audit.record(email, "ORDER_CANCELLED", "ORDER", order.getId());
		return order;
	}

	@Transactional
	public CustomerOrder fulfill(UUID orderId, String email, boolean canManageAll) {
		User actor = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("authenticated user does not exist"));
		CustomerOrder order = orders.lockDetailedById(orderId).orElseThrow(() -> new IllegalArgumentException("order not found"));
		if (!canManageAll && !order.getCreatedBy().getEmail().equals(email))
			throw new org.springframework.security.access.AccessDeniedException("order belongs to another user");
		order.fulfill(actor);
		if (audit != null) audit.record(email, "ORDER_FULFILLED", "ORDER", order.getId());
		return order;
	}

	private CustomerOrder editableOrder(UUID id, String email, boolean canManageAll) {
		CustomerOrder order = orders.lockDetailedById(id).orElseThrow(() -> new IllegalArgumentException("order not found"));
		if (!canManageAll && !order.getCreatedBy().getEmail().equals(email)) throw new org.springframework.security.access.AccessDeniedException("order belongs to another user");
		order.requireDraft();
		return order;
	}
}
