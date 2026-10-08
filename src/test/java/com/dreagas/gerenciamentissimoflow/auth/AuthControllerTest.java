package com.dreagas.gerenciamentissimoflow.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class AuthControllerTest {
	private final UserRepository users = Mockito.mock(UserRepository.class);
	private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
	private final JwtService jwt = new JwtService("test-only-signing-key-with-at-least-32-bytes", 3600,
			new org.springframework.mock.env.MockEnvironment());
	private final AuthController controller = new AuthController(users, passwords, jwt);
	private User active;

	@BeforeEach
	void setUp() {
		active = new User("Alice", "alice@example.com", passwords.encode("correct horse"), UserRole.MANAGER);
		when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(active));
	}

	@Test
	void validCredentialsReturnSignedBearerToken() {
		var response = controller.login(new AuthController.LoginRequest(" Alice@Example.com ", "correct horse"));
		assertEquals("Bearer", response.tokenType());
		assertEquals("alice@example.com", jwt.validateAndGetSubject(response.accessToken()));
	}

	@Test
	void invalidPasswordAndUnknownUserHaveSameUnauthorizedResponse() {
		ResponseStatusException wrongPassword = assertThrows(ResponseStatusException.class,
				() -> controller.login(new AuthController.LoginRequest("alice@example.com", "incorrect")));
		when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());
		ResponseStatusException missing = assertThrows(ResponseStatusException.class,
				() -> controller.login(new AuthController.LoginRequest("missing@example.com", "incorrect")));
		assertEquals(wrongPassword.getStatusCode(), missing.getStatusCode());
		assertEquals(401, wrongPassword.getStatusCode().value());
	}

	@Test
	void disabledUserCannotLogin() {
		active.disable();
		assertThrows(ResponseStatusException.class,
				() -> controller.login(new AuthController.LoginRequest("alice@example.com", "correct horse")));
	}

	@Test
	void rejectsTamperedAndExpiredTokens() {
		String token = jwt.issue(active);
		String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");
		String tamperedSignature = token.substring(0, token.lastIndexOf('.') + 1)
				+ (token.substring(token.lastIndexOf('.') + 1).startsWith("a") ? "b" : "a")
				+ token.substring(token.lastIndexOf('.') + 2);
		assertThrows(RuntimeException.class, () -> jwt.validateAndGetSubject(tamperedSignature));
		var key = Keys.hmacShaKeyFor("test-only-signing-key-with-at-least-32-bytes"
				.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		String expired = Jwts.builder().subject("alice@example.com")
				.expiration(Date.from(Instant.now().minusSeconds(10))).signWith(key).compact();
		assertThrows(RuntimeException.class, () -> jwt.validateAndGetSubject(expired));
		assertThrows(IllegalStateException.class, () -> new JwtService("test-only-signing-key-with-at-least-32-bytes", 0,
				new org.springframework.mock.env.MockEnvironment()));
	}

	@Test
	void rejectsWeakSigningSecret() {
		assertThrows(IllegalStateException.class, () -> new JwtService("short", 3600,
				new org.springframework.mock.env.MockEnvironment()));
		assertThrows(IllegalStateException.class, () -> new JwtService("change-me", 3600,
				new org.springframework.mock.env.MockEnvironment()));
	}
}
