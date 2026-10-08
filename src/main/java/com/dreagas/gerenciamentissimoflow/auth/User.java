package com.dreagas.gerenciamentissimoflow.auth;

import java.util.Locale;
import java.util.Objects;
import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User extends BaseAuditableEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserRole role;

	@Column(nullable = false)
	private boolean enabled = true;

	protected User() {
	}

	public User(String name, String email, String passwordHash, UserRole role) {
		this.name = Objects.requireNonNull(name, "name must not be null").trim();
		this.email = normalizeEmail(email);
		this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
		this.role = Objects.requireNonNull(role, "role must not be null");
	}

	@PrePersist
	@PreUpdate
	private void normalizeEmail() {
		email = normalizeEmail(email);
	}

	private static String normalizeEmail(String value) {
		return Objects.requireNonNull(value, "email must not be null").trim().toLowerCase(Locale.ROOT);
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void disable() {
		enabled = false;
	}
}
