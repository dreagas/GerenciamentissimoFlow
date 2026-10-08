package com.dreagas.gerenciamentissimoflow.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void normalizesEmailAndCanBeDisabled() {
		User user = new User("  Ada Lovelace ", " ADA@Example.COM ", "encoded-hash", UserRole.ADMIN);

		assertEquals("ada@example.com", user.getEmail());
		assertEquals("Ada Lovelace", user.getName());
		assertEquals("encoded-hash", user.getPasswordHash());
		user.disable();
		assertFalse(user.isEnabled());
	}
}
