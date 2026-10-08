package com.dreagas.gerenciamentissimoflow.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class OrderTest {
	@Test
	void confirmationConsumesStockAndWritesOrderReferencedMovement() {
		var balances = mock(com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository.class);
		var movements = mock(com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository.class);
		var product = new com.dreagas.gerenciamentissimoflow.product.Product("SKU-2", "P", null, null, BigDecimal.ONE, 0);
		var warehouse = new com.dreagas.gerenciamentissimoflow.warehouse.Warehouse("WH-2", "W", "Loc");
		var balance = new com.dreagas.gerenciamentissimoflow.inventory.InventoryBalance(product, warehouse, 5);
		when(balances.lockAllForProducts(anyCollection(), any())).thenReturn(java.util.List.of(balance));
		var service = new com.dreagas.gerenciamentissimoflow.inventory.InventoryService(balances, movements, mock(com.dreagas.gerenciamentissimoflow.product.ProductRepository.class), mock(com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository.class));
		var item = new OrderItem(product, 2, new BigDecimal("1.00"));
		service.consumeForOrder(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), java.util.List.of(item));
		assertEquals(3, balance.getQuantity());
		verify(movements).save(any(com.dreagas.gerenciamentissimoflow.inventory.InventoryMovement.class));
	}

	@Test
	void itemRequiresPositiveQuantityAndNonNegativePrice() {
		com.dreagas.gerenciamentissimoflow.product.Product product =
				new com.dreagas.gerenciamentissimoflow.product.Product("SKU-1", "Product", null, null, BigDecimal.ONE, 0);
		assertNotNull(product);
		assertThrows(IllegalArgumentException.class, () -> new OrderItem(product, 0, BigDecimal.ONE));
		assertThrows(IllegalArgumentException.class, () -> new OrderItem(product, 1, new BigDecimal("-1")));
		OrderItem item = new OrderItem(product, 2, new BigDecimal("3.50"));
		assertEquals(new BigDecimal("7.00"), item.getLineTotal());
		item.update(3, new BigDecimal("4.00"));
		assertEquals(new BigDecimal("12.00"), item.getLineTotal());
	}

	@Test
	void cancellationIsAllowedOnlyForDraftOrConfirmedAndCannotRepeatTransition() {
		var user = new com.dreagas.gerenciamentissimoflow.auth.User("A", "cancel-domain@example.test", "hash", com.dreagas.gerenciamentissimoflow.auth.UserRole.OPERATOR);
		var warehouse = new com.dreagas.gerenciamentissimoflow.warehouse.Warehouse("WC", "W", "Loc");
		var product = new com.dreagas.gerenciamentissimoflow.product.Product("SC", "P", null, null, BigDecimal.ONE, 0);
		var draft = new CustomerOrder(warehouse, user);
		draft.cancel();
		assertEquals(OrderStatus.CANCELLED, draft.getStatus());
		assertThrows(IllegalStateException.class, draft::cancel);

		var confirmed = new CustomerOrder(warehouse, user);
		confirmed.addItem(new OrderItem(product, 1, BigDecimal.ONE));
		confirmed.confirm();
		confirmed.cancel();
		assertEquals(OrderStatus.CANCELLED, confirmed.getStatus());
	}
}
