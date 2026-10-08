package com.dreagas.gerenciamentissimoflow.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.dreagas.gerenciamentissimoflow.auth.User;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.auth.UserRole;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalance;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryService;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class OrderConcurrencyIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired private OrderService service;
	@Autowired private UserRepository users;
	@Autowired private WarehouseRepository warehouses;
	@Autowired private ProductRepository products;
	@Autowired private InventoryBalanceRepository balances;
	@Autowired private InventoryMovementRepository movements;
	@Autowired private InventoryService inventory;
	@Autowired private CustomerOrderRepository orders;
	@Autowired private TransactionTemplate transactions;

	@Test
	void concurrentConfirmationsCannotOversellAndLoserRemainsDraft() throws Exception {
		String suffix = UUID.randomUUID().toString();
		User user = users.saveAndFlush(new User("Concurrent", "concurrent-" + suffix + "@example.test", "hash", UserRole.OPERATOR));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("CW-" + suffix, "Concurrent", "North"));
		Product product = products.saveAndFlush(new Product("CP-" + suffix, "Concurrent", null, null, BigDecimal.ZERO, 0));
		inventory.receive(product.getId(), warehouse.getId(), 10, "initial concurrent test stock");
		UUID first = createOrder(user, warehouse, product);
		UUID second = createOrder(user, warehouse, product);
		AtomicInteger confirmed = new AtomicInteger();
		AtomicInteger insufficient = new AtomicInteger();
		try (var pool = Executors.newFixedThreadPool(2)) {
			var start = new java.util.concurrent.CountDownLatch(1);
			Callable<Void> a = () -> { start.await(); confirm(first, user, confirmed, insufficient); return null; };
			Callable<Void> b = () -> { start.await(); confirm(second, user, confirmed, insufficient); return null; };
			var fa = pool.submit(a); var fb = pool.submit(b); start.countDown();
			fa.get(30, TimeUnit.SECONDS); fb.get(30, TimeUnit.SECONDS);
		}
		assertEquals(1, confirmed.get());
		assertEquals(1, insufficient.get());
		assertEquals(3, balances.findByProduct_IdAndWarehouse_Id(product.getId(), warehouse.getId()).orElseThrow().getQuantity());
		assertEquals(1, movements.findAll().stream().filter(m -> "ORDER_OUT".equals(m.getType())).count());
		assertEquals(1, java.util.stream.Stream.of(first, second).map(orders::findById).flatMap(java.util.Optional::stream)
				.filter(order -> order.getStatus() == OrderStatus.CONFIRMED).count());
		assertTrue(java.util.stream.Stream.of(first, second).map(orders::findById).flatMap(java.util.Optional::stream)
				.anyMatch(order -> order.getStatus() == OrderStatus.DRAFT));
	}

	private UUID createOrder(User user, Warehouse warehouse, Product product) {
		return transactions.execute(status -> {
			CustomerOrder order = orders.save(new CustomerOrder(warehouse, user));
			order.addItem(new OrderItem(product, 7, BigDecimal.ONE));
			return order.getId();
		});
	}

	private void confirm(UUID id, User user, AtomicInteger confirmed, AtomicInteger insufficient) {
		try { service.confirm(id, user.getEmail(), false); confirmed.incrementAndGet(); }
		catch (IllegalArgumentException expected) {
			if ("Insufficient stock".equals(expected.getMessage())) insufficient.incrementAndGet();
			else throw expected;
		}
	}
}
