package com.dreagas.gerenciamentissimoflow.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;

class InventoryBalanceTest {
	@Test
	void acceptsNonNegativeQuantityAndRejectsNegativeValues() {
		Product product = new Product("SKU-1", "Item", null, null, BigDecimal.ZERO, 0);
		Warehouse warehouse = new Warehouse("W1", "Main", "North");
		InventoryBalance balance = new InventoryBalance(product, warehouse, 25);
		assertEquals(25, balance.getQuantity());
		assertThrows(IllegalArgumentException.class, () -> balance.setQuantity(-1));
	}
}
