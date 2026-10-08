package com.dreagas.gerenciamentissimoflow.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoAdminSeedTest {
	@Test
	void rejectsMissingOrWeakDemoPasswordBeforeDatabaseAccess() {
		UserRepository users = mock(UserRepository.class);
		PasswordEncoder encoder = mock(PasswordEncoder.class);
		DemoAdminSeed seed = new DemoAdminSeed(users, encoder, "demo@example.invalid", "");

		assertThrows(IllegalStateException.class, () -> seed.run());
		verifyNoInteractions(users, encoder);
	}

	@Test
	void seedsMissingAdminAndLeavesExistingUserUntouched() {
		UserRepository users = mock(UserRepository.class);
		PasswordEncoder encoder = mock(PasswordEncoder.class);
		when(users.findByEmail("demo@example.invalid")).thenReturn(java.util.Optional.empty());
		when(encoder.encode(anyString())).thenReturn("encoded");
		DemoAdminSeed seed = new DemoAdminSeed(users, encoder, "demo@example.invalid", "Safe demo password 2026!");

		seed.run();
		verify(users).save(any(User.class));
		verify(encoder).encode("Safe demo password 2026!");

		org.mockito.Mockito.clearInvocations(users, encoder);
		User existing = new User("Existing", "demo@example.invalid", "existing-hash", UserRole.ADMIN);
		when(users.findByEmail("demo@example.invalid")).thenReturn(java.util.Optional.of(existing));
		seed.run();
		verify(users, never()).save(any(User.class));
	}
}
