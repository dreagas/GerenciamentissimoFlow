package com.dreagas.gerenciamentissimoflow.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class UserRepositoryIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void configureDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private UserRepository userRepository;

	@Test
	@Transactional
	void persistsUserWithNormalizedEmail() {
		User saved = userRepository.saveAndFlush(
				new User("Test User", "  Test@Example.COM ", "encoded-password", UserRole.OPERATOR));

		assertEquals("test@example.com", saved.getEmail());
	}

	@Test
	@Transactional
	void rejectsDuplicateEmail() {
		userRepository.saveAndFlush(new User("First", "same@example.com", "hash-one", UserRole.ADMIN));
		userRepository.save(new User("Second", " SAME@example.com ", "hash-two", UserRole.MANAGER));

		assertThrows(DataIntegrityViolationException.class, userRepository::flush);
	}

	@Test
	@Transactional
	void adminUserListingResponseDoesNotExposePasswordHash() {
		User saved = userRepository.saveAndFlush(
				new User("Admin listing", "listing@example.com", "secret-hash", UserRole.OPERATOR));

		AdminUserController.AdminUserResponse response = new AdminUserController.AdminUserResponse(
				saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());

		assertEquals(saved.getId(), response.id());
		assertEquals(UserRole.OPERATOR, response.role());
		assertFalse(java.util.Arrays.stream(response.getClass().getRecordComponents())
				.anyMatch(component -> component.getName().toLowerCase().contains("password")));
	}
}
