package com.dreagas.gerenciamentissimoflow.category;

import java.util.Locale;
import java.util.Objects;

import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "category")
public class Category extends BaseAuditableEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false)
	private boolean active = true;

	protected Category() {
	}

	public Category(String name) {
		this.name = normalizeName(name);
	}

	@PrePersist
	@PreUpdate
	private void normalizeName() {
		name = normalizeName(name);
	}

	public void rename(String name) {
		this.name = normalizeName(name);
	}

	public void deactivate() {
		active = false;
	}

	public String getName() {
		return name;
	}

	public boolean isActive() {
		return active;
	}

	private static String normalizeName(String name) {
		String normalized = Objects.requireNonNull(name, "name must not be null").trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		return normalized.toLowerCase(Locale.ROOT);
	}
}
