package com.dreagas.gerenciamentissimoflow.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.dreagas.gerenciamentissimoflow.auth.User;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.auth.UserRole;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class AuditEventIntegrationTest {
	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired private AuditService audit;
	@Autowired private AuditEventRepository events;
	@Autowired private UserRepository users;

	@Test
	void persistsSafeActorAndActionMetadataAndSupportsFiltering() {
		String email = "audit-" + UUID.randomUUID() + "@example.test";
		User actor = users.saveAndFlush(new User("Audit actor", email, "not-recorded", UserRole.ADMIN));
		UUID entityId = UUID.randomUUID();

		audit.record(email, "PRODUCT_UPDATED", "PRODUCT", entityId);

		var page = audit.search(actor.getId(), "PRODUCT_UPDATED", "PRODUCT", PageRequest.of(0, 10));
		assertEquals(1, page.getTotalElements());
		AuditEvent event = page.getContent().getFirst();
		assertEquals(actor.getId(), event.getActorId());
		assertEquals(email, event.getActorEmail());
		assertEquals("PRODUCT_UPDATED", event.getAction());
		assertEquals("PRODUCT", event.getEntityType());
		assertEquals(entityId, event.getEntityId());
		assertNotNull(event.getOccurredAt());
		assertEquals(0, events.search(actor.getId(), "ORDER_CONFIRMED", null, PageRequest.of(0, 10)).getTotalElements());
	}
}
