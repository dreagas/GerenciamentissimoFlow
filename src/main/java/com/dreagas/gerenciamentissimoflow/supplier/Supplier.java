package com.dreagas.gerenciamentissimoflow.supplier;

import java.util.Locale;
import java.util.Objects;

import com.dreagas.gerenciamentissimoflow.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "supplier")
public class Supplier extends BaseAuditableEntity {
	@Column(nullable = false, length = 180)
	private String name;
	@Column(length = 40)
	private String document;
	@Column(length = 320)
	private String email;
	@Column(length = 40)
	private String phone;
	@Column(nullable = false)
	private boolean active = true;

	protected Supplier() { }

	public Supplier(String name, String document, String email, String phone) {
		update(name, document, email, phone);
	}

	public void update(String name, String document, String email, String phone) {
		this.name = requireText(name, "name");
		this.document = blankToNull(document);
		this.email = email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
		this.phone = blankToNull(phone);
	}

	@PrePersist @PreUpdate
	private void normalize() { update(name, document, email, phone); }

	public void deactivate() { active = false; }
	public String getName() { return name; }
	public String getDocument() { return document; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public boolean isActive() { return active; }

	private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
	private static String requireText(String value, String field) {
		String text = Objects.requireNonNull(value, field + " must not be null").trim();
		if (text.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
		return text;
	}
}
