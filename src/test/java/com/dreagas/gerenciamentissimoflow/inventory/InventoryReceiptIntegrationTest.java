package com.dreagas.gerenciamentissimoflow.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class InventoryReceiptIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired private InventoryService service;
	@Autowired private ProductRepository products;
	@Autowired private WarehouseRepository warehouses;
	@Autowired private InventoryMovementRepository movements;

	@Test @Transactional
	void receiptAtomicallyCreatesBalanceAndMovement() {
		Product product = products.saveAndFlush(new Product("RCPT-1", "Receipt item", null, null, BigDecimal.ZERO, 0));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("RCPT-W1", "Main", "North"));		var balance = service.receive(product.getId(), warehouse.getId(), 6, "purchase order 7");
		assertEquals(6, balance.getQuantity());
		assertEquals(1, movements.count());
		assertEquals("RECEIPT", movements.findAll().getFirst().getType());
	}
}
