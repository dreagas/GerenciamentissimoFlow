package com.dreagas.gerenciamentissimoflow.warehouse;

import java.util.Locale;
import java.util.Objects;

import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse")
public class Warehouse extends BaseAuditableEntity {
	@Column(nullable = false, length = 40, unique = true)
	private String code;
	@Column(nullable = false, length = 120)
	private String name;
	@Column(nullable = false, length = 240)
	private String location;
	@Column(nullable = false)
	private boolean active = true;

	protected Warehouse() { }

	public Warehouse(String code, String name, String location) {
		update(code, name, location);
	}

	public void update(String code, String name, String location) {
		this.code = requireText(code, "code").toUpperCase(Locale.ROOT);
		this.name = requireText(name, "name");
		this.location = requireText(location, "location");
	}

	@PrePersist @PreUpdate
	private void normalize() { update(code, name, location); }

	public void deactivate() { active = false; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public String getLocation() { return location; }
	public boolean isActive() { return active; }

	private static String requireText(String value, String field) {
		String text = Objects.requireNonNull(value, field + " must not be null").trim();
		if (text.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
		return text;
	}
}
