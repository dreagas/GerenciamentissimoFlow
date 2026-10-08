package com.dreagas.gerenciamentissimoflow.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import java.util.Map;

class JwtConfigurationTest {
	@Test
	void demoProfileFailsClosedWithoutExternallyProvidedSigningSecret() {
		assertThrows(IllegalStateException.class, () -> new JwtService("", 3600,
				profileEnvironment("demo")));
	}

	@Test
	void demoProfileAcceptsExternallyProvidedSigningSecret() {
		assertDoesNotThrow(() -> new JwtService("externally-provided-demo-signing-secret-32", 3600,
				profileEnvironment("demo")));
	}

	@Test
	void productionProfileRequiresSigningSecret() {
		var environment = new StandardEnvironment();
		environment.setActiveProfiles("prod");
		assertThrows(IllegalStateException.class,
				() -> new JwtService("", 3600, environment));
	}

	private StandardEnvironment profileEnvironment(String profile) {
		var environment = new StandardEnvironment();
		environment.setActiveProfiles(profile);
		return environment;
	}

}
