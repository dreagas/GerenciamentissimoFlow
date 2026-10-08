package com.dreagas.gerenciamentissimoflow.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;

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
import com.dreagas.gerenciamentissimoflow.inventory.InventoryService;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrder;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.order.OrderItem;
import com.dreagas.gerenciamentissimoflow.order.OrderService;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class DashboardIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired DashboardService dashboard;
	@Autowired ProductRepository products;
	@Autowired WarehouseRepository warehouses;
	@Autowired InventoryService inventory;
	@Autowired UserRepository users;
	@Autowired CustomerOrderRepository orders;
	@Autowired OrderService orderService;

	@Test
	void summaryUsesCurrentDatabaseCountsAndRecentMovements() {
		String suffix = UUID.randomUUID().toString();
		User user = users.saveAndFlush(new User("Dash", "dash-" + suffix + "@example.test", "hash", UserRole.OPERATOR));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("DW-" + suffix, "Dashboard", "Center"));
		Product product = products.saveAndFlush(new Product("DP-" + suffix, "Dashboard", null, null, BigDecimal.ONE, 2));
		inventory.receive(product.getId(), warehouse.getId(), 2, "dashboard receipt");
		CustomerOrder draft = orders.saveAndFlush(new CustomerOrder(warehouse, user));
		draft.addItem(new OrderItem(product, 1, BigDecimal.ONE));
		orders.saveAndFlush(draft);
		CustomerOrder confirmed = orders.saveAndFlush(new CustomerOrder(warehouse, user));
		confirmed.addItem(new OrderItem(product, 1, BigDecimal.ONE));
		orders.saveAndFlush(confirmed);
		orderService.confirm(confirmed.getId(), user.getEmail(), false, "dash-confirm-" + suffix);

		var summary = dashboard.summary();
		assertEquals(1, summary.activeProducts());
		assertEquals(0, summary.outOfStockBalances());
		assertEquals(1, summary.lowStockBalances());
		assertEquals(1, summary.draftOrders());
		assertEquals(1, summary.confirmedOrders());
		assertEquals("ORDER_OUT", summary.recentMovements().get(0).type());
	}
}
