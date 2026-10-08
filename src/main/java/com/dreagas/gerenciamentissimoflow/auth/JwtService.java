package com.dreagas.gerenciamentissimoflow.auth;

import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private final SecretKey key;
	private final long expirationSeconds;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds, Environment environment) {
		if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32
				|| "change-me".equalsIgnoreCase(secret.trim())) {
			throw new IllegalStateException("app.jwt.secret must contain at least 32 UTF-8 bytes");
		}
		if (expirationSeconds <= 0) {
			throw new IllegalStateException("app.jwt.expiration-seconds must be positive");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		this.expirationSeconds = expirationSeconds;
	}

	public String issue(User user) {
		Instant now = Instant.now();
		return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole().name())
				.issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
				.signWith(key).compact();
	}

	public String validateAndGetSubject(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
	}
}
