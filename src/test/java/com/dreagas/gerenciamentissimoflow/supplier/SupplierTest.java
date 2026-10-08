package com.dreagas.gerenciamentissimoflow.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SupplierTest {
	@Test
	void normalizesOptionalDataAndSupportsDeactivation() {
		Supplier supplier = new Supplier(" Acme ", " ", " SALES@ACME.COM ", null);
		assertEquals("Acme", supplier.getName());
		assertNull(supplier.getDocument());
		assertEquals("sales@acme.com", supplier.getEmail());
		supplier.deactivate();
		assertFalse(supplier.isActive());
	}
	@Test
	void requiresName() {
		assertThrows(IllegalArgumentException.class, () -> new Supplier(" ", null, null, null));
	}
}
