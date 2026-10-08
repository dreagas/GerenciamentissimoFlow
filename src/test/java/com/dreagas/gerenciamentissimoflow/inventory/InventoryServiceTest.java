package com.dreagas.gerenciamentissimoflow.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

class InventoryServiceTest {
	@Test
	void rejectsNonPositiveReceiptsBeforePersistence() {
		InventoryService service = new InventoryService(Mockito.mock(InventoryBalanceRepository.class),
				Mockito.mock(InventoryMovementRepository.class), Mockito.mock(ProductRepository.class), Mockito.mock(WarehouseRepository.class));
		assertThrows(IllegalArgumentException.class, () -> service.receive(UUID.randomUUID(), UUID.randomUUID(), 0, null));
	}

	@Test
	void manualAdjustmentRequiresReasonAndMayNotGoNegative() {
		var service = new InventoryService(Mockito.mock(InventoryBalanceRepository.class), Mockito.mock(InventoryMovementRepository.class),
				Mockito.mock(ProductRepository.class), Mockito.mock(WarehouseRepository.class));
		assertThrows(IllegalArgumentException.class, () -> service.adjust(UUID.randomUUID(), UUID.randomUUID(), 0, " "));
		assertThrows(IllegalArgumentException.class, () -> service.adjust(UUID.randomUUID(), UUID.randomUUID(), -1, "count correction"));
	}

	@Test
	void increasesBalanceAndWritesMovement() {
		var balances = Mockito.mock(InventoryBalanceRepository.class);
		var movements = Mockito.mock(InventoryMovementRepository.class);
		var products = Mockito.mock(ProductRepository.class);
		var warehouses = Mockito.mock(WarehouseRepository.class);
		Product product = new Product("SVC-1", "Item", null, null, BigDecimal.ZERO, 0);
		Warehouse warehouse = new Warehouse("SVC-W1", "Main", "North");
		when(products.findById(any())).thenReturn(Optional.of(product));
		when(warehouses.findById(any())).thenReturn(Optional.of(warehouse));
		when(balances.lockByProductAndWarehouse(any(), any())).thenReturn(Optional.of(new InventoryBalance(product, warehouse, 4)));
		when(balances.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		var service = new InventoryService(balances, movements, products, warehouses);
		assertEquals(9, service.receive(UUID.randomUUID(), UUID.randomUUID(), 5, "restock").getQuantity());
		verify(movements).save(any(InventoryMovement.class));
	}
}
