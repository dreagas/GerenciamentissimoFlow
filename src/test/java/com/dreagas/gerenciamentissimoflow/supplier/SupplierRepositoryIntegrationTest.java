package com.dreagas.gerenciamentissimoflow.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class SupplierRepositoryIntegrationTest {
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
	@Autowired private SupplierRepository repository;
	@Test @Transactional
	void persistsSupplierAndSearchesWithPageable() {
		repository.save(new Supplier("Acme Components", "DOC-1", "sales@acme.example", null));
		repository.save(new Supplier("Other", null, null, null));
		var page = repository.findAllByNameContainingIgnoreCaseOrDocumentContainingIgnoreCaseOrEmailContainingIgnoreCase(
				"acme", "acme", "acme", org.springframework.data.domain.PageRequest.of(0, 10));
		assertEquals(1, page.getTotalElements());
	}
}
