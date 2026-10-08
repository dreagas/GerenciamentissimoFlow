package com.dreagas.gerenciamentissimoflow.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
class InventoryBalanceRepositoryIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired private InventoryBalanceRepository balances;
	@Autowired private ProductRepository products;
	@Autowired private WarehouseRepository warehouses;

	@Test @Transactional
	void enforcesOneNonNegativeBalancePerProductAndWarehouse() {
		Product product = products.saveAndFlush(new Product("INV-1", "Inventory item", null, null, BigDecimal.ZERO, 0));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("INV-W1", "Main", "North"));
		balances.saveAndFlush(new InventoryBalance(product, warehouse, 7));
		assertEquals(7, balances.findByProduct_IdAndWarehouse_Id(product.getId(), warehouse.getId()).orElseThrow().getQuantity());
		assertThrows(DataIntegrityViolationException.class,
				() -> balances.saveAndFlush(new InventoryBalance(product, warehouse, 2)));
	}

	@Test @Transactional
	void databaseRejectsNegativeQuantity() {
		Product product = products.saveAndFlush(new Product("INV-2", "Another item", null, null, BigDecimal.ZERO, 0));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("INV-W2", "Second", "South"));
		InventoryBalance balance = balances.saveAndFlush(new InventoryBalance(product, warehouse, 1));
		assertThrows(DataIntegrityViolationException.class, () -> {
			balance.setQuantity(0);
			org.springframework.test.util.ReflectionTestUtils.setField(balance, "quantity", -1L);
			balances.saveAndFlush(balance);
		});
	}

	@Test @Transactional
	void searchSupportsStockFlagsAndProductWarehouseFilters() {
		Product product = products.saveAndFlush(new Product("INV-3", "Low stock item", null, null, BigDecimal.ZERO, 5));
		Warehouse warehouse = warehouses.saveAndFlush(new Warehouse("INV-W3", "Search warehouse", "East"));
		balances.saveAndFlush(new InventoryBalance(product, warehouse, 0));
		var page = balances.search(product.getId(), warehouse.getId(), null, true, true,
				org.springframework.data.domain.PageRequest.of(0, 10));
		assertEquals(1, page.getTotalElements());
	}
}
