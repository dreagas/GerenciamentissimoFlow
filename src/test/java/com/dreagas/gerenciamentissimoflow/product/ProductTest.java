package com.dreagas.gerenciamentissimoflow.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {
	@Test
	void normalizesSkuAndSoftDeactivates() {
		Product product = new Product(" sku-1 ", " Widget ", null, null, new BigDecimal("2.50"), 0);
		assertEquals("SKU-1", product.getSku());
		product.deactivate();
		assertFalse(product.isActive());
	}

	@Test
	void rejectsNegativePriceAndMinimumStock() {
		assertThrows(IllegalArgumentException.class,
				() -> new Product("A", "Widget", null, null, new BigDecimal("-1.00"), 0));
		assertThrows(IllegalArgumentException.class,
				() -> new Product("A", "Widget", null, null, BigDecimal.ONE, -1));
	}
}
