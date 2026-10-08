package com.dreagas.gerenciamentissimoflow.shared.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BaseAuditableEntityTest {

	private static final class TestEntity extends BaseAuditableEntity {
		void create() {
			onCreate();
		}

		void update() {
			onUpdate();
		}
	}

	@Test
	void lifecycleMaintainsIdentifierAndUtcTimestamps() throws InterruptedException {
		TestEntity entity = new TestEntity();
		entity.create();
		Instant created = entity.getCreatedAt();
		assertNotNull(entity.getId());
		assertEquals(created, entity.getUpdatedAt());

		Thread.sleep(2);
		entity.update();

		assertEquals(created, entity.getCreatedAt());
		assertTrue(entity.getUpdatedAt().isAfter(created));
	}
}
