package com.dreagas.gerenciamentissimoflow.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
class WarehouseRepositoryIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired private WarehouseRepository repository;
	@Test @Transactional
	void persistsSearchableWarehouseWithCaseInsensitiveUniqueCode() {
		repository.saveAndFlush(new Warehouse(" WH-01 ", "Main warehouse", "North building"));
		assertEquals(1, repository.findAllByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
				"wh-0", "wh-0", "wh-0", org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
		assertThrows(DataIntegrityViolationException.class,
				() -> repository.saveAndFlush(new Warehouse("wh-01", "Second", "South")));
	}
}
