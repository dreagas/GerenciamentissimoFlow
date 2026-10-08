package com.dreagas.gerenciamentissimoflow.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WarehouseTest {
	@Test
	void normalizesCodeAndTrimsFields() {
		Warehouse warehouse = new Warehouse(" wh-01 ", " Main ", " North building ");
		assertEquals("WH-01", warehouse.getCode());
		assertEquals("Main", warehouse.getName());
		assertEquals("North building", warehouse.getLocation());
	}

	@Test
	void requiresAllWarehouseFields() {
		assertThrows(IllegalArgumentException.class, () -> new Warehouse(" ", "Name", "Location"));
		assertThrows(IllegalArgumentException.class, () -> new Warehouse("W1", " ", "Location"));
		assertThrows(IllegalArgumentException.class, () -> new Warehouse("W1", "Name", " "));
	}

	@Test
	void deactivationIsSoftAndIdempotent() {
		Warehouse warehouse = new Warehouse("W1", "Main", "North");
		warehouse.deactivate();
		warehouse.deactivate();
		assertFalse(warehouse.isActive());
	}
}
