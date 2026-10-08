package com.dreagas.gerenciamentissimoflow.category;

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
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class CategoryRepositoryIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void configureDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private CategoryRepository repository;

	@Test
	@Transactional
	void persistsNormalizedNameAndActiveState() {
		Category saved = repository.saveAndFlush(new Category("  Office supplies "));

		assertEquals("office supplies", saved.getName());
	}

	@Test
	@Transactional
	void databaseEnforcesUniqueCategoryNameRegardlessOfCase() {
		repository.saveAndFlush(new Category("Office supplies"));
		repository.save(new Category("OFFICE SUPPLIES"));

		assertThrows(DataIntegrityViolationException.class, repository::flush);
	}

	@Test
	@Transactional
	void searchesByNameAndActiveStateWithPagination() {
		repository.save(new Category("Office supplies"));
		Category inactive = new Category("Office furniture");
		inactive.deactivate();
		repository.save(inactive);
		repository.save(new Category("Food"));

		var page = repository.findAllByNameContainingIgnoreCaseAndActive("office", true, PageRequest.of(0, 1));

		assertEquals(1, page.getTotalElements());
		assertEquals(1, page.getContent().size());
		assertEquals("office supplies", page.getContent().getFirst().getName());
	}
}
