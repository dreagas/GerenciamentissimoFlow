package com.dreagas.gerenciamentissimoflow.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.dreagas.gerenciamentissimoflow.auth.User;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.auth.UserRole;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryService;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class OrderIdempotencyIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired private OrderService service;
	@Autowired private CustomerOrderRepository orders;
	@Autowired private UserRepository users;
	@Autowired private WarehouseRepository warehouses;
	@Autowired private ProductRepository products;
	@Autowired private InventoryService inventory;
	@Autowired private InventoryMovementRepository movements;

	@Test
	void retryAndConcurrentSameKeyConsumeOnlyOnceAndDifferentOrderConflicts() throws Exception {
		String suffix = UUID.randomUUID().toString();
		User user = users.saveAndFlush(new User("Idempotent", "idem-" + suffix + "@example.test", "hash", UserRole.OPERATOR));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("IW-" + suffix, "Idempotent", "North"));
		Product product = products.saveAndFlush(new Product("IP-" + suffix, "Idempotent", null, null, BigDecimal.ZERO, 0));
		inventory.receive(product.getId(), warehouse.getId(), 10, "idempotency stock");
		UUID orderId = create(user, warehouse, product);
		AtomicInteger successes = new AtomicInteger();
		try (var pool = Executors.newFixedThreadPool(2)) {
			CountDownLatch start = new CountDownLatch(1);
			var one = pool.submit(() -> { start.await(); service.confirm(orderId, user.getEmail(), false, "retry-key-" + suffix); successes.incrementAndGet(); return null; });
			var two = pool.submit(() -> { start.await(); service.confirm(orderId, user.getEmail(), false, "retry-key-" + suffix); successes.incrementAndGet(); return null; });
			start.countDown(); one.get(30, TimeUnit.SECONDS); two.get(30, TimeUnit.SECONDS);
		}
		assertEquals(2, successes.get());
		assertEquals(OrderStatus.CONFIRMED, orders.findById(orderId).orElseThrow().getStatus());
		assertEquals(3, inventoryBalance(product, warehouse));
		assertEquals(1, movements.countByReferenceIdAndType(orderId, "ORDER_OUT"));
		UUID differentOrder = create(user, warehouse, product);
		assertThrows(IdempotencyConflictException.class,
				() -> service.confirm(differentOrder, user.getEmail(), false, "retry-key-" + suffix));
		assertEquals(OrderStatus.DRAFT, orders.findById(differentOrder).orElseThrow().getStatus());
	}

	@Test
	void cancellingConfirmedOrderRestoresStockOnceAndDraftCancellationDoesNotTouchStock() {
		String suffix = UUID.randomUUID().toString();
		User user = users.saveAndFlush(new User("Cancel", "cancel-" + suffix + "@example.test", "hash", UserRole.OPERATOR));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("CW-" + suffix, "Cancel", "South"));
		Product product = products.saveAndFlush(new Product("CP-" + suffix, "Cancel", null, null, BigDecimal.ZERO, 0));
		inventory.receive(product.getId(), warehouse.getId(), 20, "cancellation stock");
		UUID draftId = createWithQuantity(user, warehouse, product, 4);
		service.cancel(draftId, user.getEmail(), false);
		assertEquals(OrderStatus.CANCELLED, orders.findById(draftId).orElseThrow().getStatus());
		assertEquals(20, inventoryBalance(product, warehouse));

		UUID confirmedId = createWithQuantity(user, warehouse, product, 6);
		service.confirm(confirmedId, user.getEmail(), false, "confirm-before-cancel-" + suffix);
		assertEquals(14, inventoryBalance(product, warehouse));
		service.cancel(confirmedId, user.getEmail(), false);
		service.cancel(confirmedId, user.getEmail(), false);
		assertEquals(OrderStatus.CANCELLED, orders.findById(confirmedId).orElseThrow().getStatus());
		assertEquals(20, inventoryBalance(product, warehouse));
		assertEquals(1, movements.countByReferenceIdAndType(confirmedId, "ORDER_CANCEL_IN"));
		assertEquals(1, movements.countByReferenceIdAndType(confirmedId, "ORDER_OUT"));
	}

	@Test
	void failedConfirmedOrderCancellationRollsBackStatusAndAllStockChanges() {
		String suffix = UUID.randomUUID().toString();
		User user = users.saveAndFlush(new User("Rollback", "rollback-" + suffix + "@example.test", "hash", UserRole.OPERATOR));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("RW-" + suffix, "Rollback", "East"));
		Product product = products.saveAndFlush(new Product("RP-" + suffix, "Rollback", null, null, BigDecimal.ZERO, 0));
		inventory.receive(product.getId(), warehouse.getId(), 10, "rollback stock");
		UUID orderId = createWithQuantity(user, warehouse, product, 3);
		service.confirm(orderId, user.getEmail(), false, "confirm-rollback-" + suffix);
		var balance = inventoryBalanceRepository.findByProduct_IdAndWarehouse_Id(product.getId(), warehouse.getId()).orElseThrow();
		balance.setQuantity(Long.MAX_VALUE);
		inventoryBalanceRepository.saveAndFlush(balance);
		assertThrows(ArithmeticException.class, () -> service.cancel(orderId, user.getEmail(), false));
		assertEquals(OrderStatus.CONFIRMED, orders.findById(orderId).orElseThrow().getStatus());
		assertEquals(Long.MAX_VALUE, inventoryBalance(product, warehouse));
		assertEquals(0, movements.countByReferenceIdAndType(orderId, "ORDER_CANCEL_IN"));
	}

	@Test
	void fulfillmentRequiresConfirmedOrderAndRecordsActorAndTimeWithoutChangingStock() {
		String suffix = UUID.randomUUID().toString();
		User user = users.saveAndFlush(new User("Fulfill", "fulfill-" + suffix + "@example.test", "hash", UserRole.OPERATOR));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("FW-" + suffix, "Fulfill", "West"));
		Product product = products.saveAndFlush(new Product("FP-" + suffix, "Fulfill", null, null, BigDecimal.ZERO, 0));
		inventory.receive(product.getId(), warehouse.getId(), 10, "fulfillment stock");
		UUID orderId = createWithQuantity(user, warehouse, product, 4);
		assertThrows(IllegalStateException.class, () -> service.fulfill(orderId, user.getEmail(), false));
		service.confirm(orderId, user.getEmail(), false, "confirm-before-fulfill-" + suffix);
		long stockAfterConfirmation = inventoryBalance(product, warehouse);
		CustomerOrder fulfilled = service.fulfill(orderId, user.getEmail(), false);
		assertEquals(OrderStatus.FULFILLED, fulfilled.getStatus());
		assertEquals(user.getId(), fulfilled.getFulfilledBy().getId());
		org.junit.jupiter.api.Assertions.assertNotNull(fulfilled.getFulfilledAt());
		assertEquals(stockAfterConfirmation, inventoryBalance(product, warehouse));
		assertThrows(IllegalStateException.class, () -> service.fulfill(orderId, user.getEmail(), false));
		assertThrows(IllegalStateException.class, () -> service.cancel(orderId, user.getEmail(), false));
	}

	private long inventoryBalance(Product product, Warehouse warehouse) {
		return inventoryBalanceRepository.findByProduct_IdAndWarehouse_Id(product.getId(), warehouse.getId()).orElseThrow().getQuantity();
	}
	@Autowired private com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository inventoryBalanceRepository;
	private UUID create(User user, Warehouse warehouse, Product product) {
		return createWithQuantity(user, warehouse, product, 7);
	}
	private UUID createWithQuantity(User user, Warehouse warehouse, Product product, long quantity) {
		CustomerOrder order = orders.saveAndFlush(new CustomerOrder(warehouse, user));
		order.addItem(new OrderItem(product, quantity, BigDecimal.ONE));
		return orders.saveAndFlush(order).getId();
	}
}
