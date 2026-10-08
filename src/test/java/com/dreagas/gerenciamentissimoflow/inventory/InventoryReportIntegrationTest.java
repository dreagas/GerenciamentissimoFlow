package com.dreagas.gerenciamentissimoflow.inventory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.io.StringWriter;
import java.util.Locale;
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

import com.dreagas.gerenciamentissimoflow.category.Category;
import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class InventoryReportIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired InventoryBalanceRepository balances;
	@Autowired InventoryCsvExporter exporter;
	@Autowired CategoryRepository categories;
	@Autowired ProductRepository products;
	@Autowired WarehouseRepository warehouses;
	@Autowired InventoryService inventory;

	@Test
	void exportsFilteredStockPositionAsSafeUtf8Csv() throws Exception {
		String suffix = UUID.randomUUID().toString();
		Category category = categories.saveAndFlush(new Category("Report " + suffix));
		Warehouse selectedWarehouse = warehouses.saveAndFlush(new Warehouse("RW-" + suffix, "Report", "North"));
		Warehouse otherWarehouse = warehouses.saveAndFlush(new Warehouse("RW2-" + suffix, "Other", "South"));
		Product riskyName = products.saveAndFlush(new Product("RS-" + suffix, "=1+1,\"Widget\"", null, category,
				BigDecimal.ONE, 5));
		Product otherProduct = products.saveAndFlush(new Product("RO-" + suffix, "Other item", null, category, BigDecimal.ONE, 1));
		inventory.receive(riskyName.getId(), selectedWarehouse.getId(), 2, "report data");
		inventory.receive(otherProduct.getId(), selectedWarehouse.getId(), 10, "report data");
		inventory.receive(riskyName.getId(), otherWarehouse.getId(), 0 + 3, "report data");

		StringWriter csv = new StringWriter();
		exporter.write(selectedWarehouse.getId(), category.getId(), true, csv);
		assertTrue(csv.toString().startsWith("sku,product_name,warehouse_code,quantity,minimum_stock,low_stock\r\n"));
		assertTrue(csv.toString().contains("\"'=1+1,\"\"Widget\"\"\""), csv.toString());
		assertTrue(csv.toString().contains("RS-" + suffix.toUpperCase(Locale.ROOT)), csv.toString());
		assertTrue(csv.toString().contains("RW-" + suffix.toUpperCase(Locale.ROOT)));
		assertTrue(!csv.toString().contains("RW2-" + suffix.toUpperCase(Locale.ROOT)));
		assertTrue(!csv.toString().contains("Other item"));
	}
}
