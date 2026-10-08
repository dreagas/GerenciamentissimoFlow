package com.dreagas.gerenciamentissimoflow.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

class InventoryRulesTest {
	@Test
	void rejectsInactiveReferencesWithoutWritingStockMovement() {
		var balances = mock(InventoryBalanceRepository.class);
		var movements = mock(InventoryMovementRepository.class);
		var products = mock(ProductRepository.class);
		var warehouses = mock(WarehouseRepository.class);
		Product inactive = new Product("INACTIVE", "Inactive", null, null, BigDecimal.ONE, 0);
		inactive.deactivate();
		when(products.findById(any())).thenReturn(Optional.of(inactive));
		var service = new InventoryService(balances, movements, products, warehouses);

		assertThrows(IllegalArgumentException.class,
				() -> service.receive(UUID.randomUUID(), UUID.randomUUID(), 4, "restock"));
		verify(balances, never()).saveAndFlush(any());
		verify(movements, never()).save(any());
	}

	@Test
	void adjustmentRecordsTheActualDeltaAndRejectsNoOp() {
		var balances = mock(InventoryBalanceRepository.class);
		var movements = mock(InventoryMovementRepository.class);
		var products = mock(ProductRepository.class);
		var warehouses = mock(WarehouseRepository.class);
		Product product = new Product("ADJUST", "Adjustable", null, null, BigDecimal.ONE, 0);
		Warehouse warehouse = new Warehouse("ADJUST-W", "Adjust warehouse", "Local");
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		when(products.findById(productId)).thenReturn(Optional.of(product));
		when(warehouses.findById(warehouseId)).thenReturn(Optional.of(warehouse));
		InventoryBalance balance = new InventoryBalance(product, warehouse, 9);
		when(balances.lockByProductAndWarehouse(productId, warehouseId)).thenReturn(Optional.of(balance));
		when(balances.saveAndFlush(balance)).thenReturn(balance);
		InventoryService service = new InventoryService(balances, movements, products, warehouses);

		assertEquals(4, service.adjust(productId, warehouseId, 4, "physical count").getQuantity());
		var movement = org.mockito.ArgumentCaptor.forClass(InventoryMovement.class);
		verify(movements).save(movement.capture());
		assertEquals("ADJUSTMENT", movement.getValue().getType());
		assertEquals(5, movement.getValue().getQuantity());
		assertThrows(IllegalArgumentException.class,
				() -> service.adjust(productId, warehouseId, 4, "physical count"));
	}
}
