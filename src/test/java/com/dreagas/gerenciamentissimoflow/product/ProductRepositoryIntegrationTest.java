package com.dreagas.gerenciamentissimoflow.product;

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

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class ProductRepositoryIntegrationTest {
	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired private ProductRepository products;

	@Test
	@Transactional
	void persistsProductWithNormalizedSkuAndMoney() {
		Product product = products.saveAndFlush(new Product("sku-22", "Part", null, null,
				new BigDecimal("12.3400"), 5));
		assertEquals("SKU-22", product.getSku());
		assertEquals(new BigDecimal("12.3400"), product.getReferencePrice());
	}

	@Test
	@Transactional
	void databaseEnforcesSkuUniqueness() {
		products.saveAndFlush(new Product("SKU-23", "One", null, null, BigDecimal.ONE, 0));
		products.save(new Product("SKU-23", "Two", null, null, BigDecimal.ONE, 0));
		assertThrows(DataIntegrityViolationException.class, products::flush);
	}
}
