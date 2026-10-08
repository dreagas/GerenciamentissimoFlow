package com.dreagas.gerenciamentissimoflow.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.dreagas.gerenciamentissimoflow.auth.User;
import com.dreagas.gerenciamentissimoflow.auth.UserRole;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;

class CustomerOrderTest {
	private final User actor = new User("Test", "order-domain@example.test", "hash", UserRole.OPERATOR);
	private final Warehouse warehouse = new Warehouse("DOMAIN", "Domain warehouse", "Local");
	private final Product product = new Product("DOMAIN-1", "Domain product", null, null, BigDecimal.ONE, 0);

	@Test
	void calculatesAndRecalculatesTotalAndLocksItemsAfterConfirmation() {
		CustomerOrder order = new CustomerOrder(warehouse, actor);
		OrderItem first = new OrderItem(product, 2, new BigDecimal("3.25"));
		OrderItem second = new OrderItem(product, 1, new BigDecimal("0.75"));
		order.addItem(first);
		order.addItem(second);
		assertEquals(new BigDecimal("7.2500"), order.getTotalAmount());

		order.recalculateItem(first, 3, new BigDecimal("2.10"));
		assertEquals(new BigDecimal("7.0500"), order.getTotalAmount());
		order.removeItem(second);
		assertEquals(new BigDecimal("6.3000"), order.getTotalAmount());
		assertEquals(1, order.getItems().size());

		order.confirm();
		assertEquals(OrderStatus.CONFIRMED, order.getStatus());
		assertThrows(IllegalStateException.class, () -> order.addItem(new OrderItem(product, 1, BigDecimal.ONE)));
	}

	@Test
	void rejectsConfirmationWithoutItemsAndInvalidFulfillmentTransitions() {
		CustomerOrder order = new CustomerOrder(warehouse, actor);
		assertThrows(IllegalStateException.class, order::confirm);
		assertThrows(IllegalStateException.class, () -> order.fulfill(actor));
		order.addItem(new OrderItem(product, 1, BigDecimal.ONE));
		order.confirm();
		order.fulfill(actor);
		assertEquals(OrderStatus.FULFILLED, order.getStatus());
		assertEquals(actor, order.getFulfilledBy());
		assertThrows(IllegalStateException.class, () -> order.fulfill(actor));
		assertThrows(IllegalStateException.class, order::cancel);
	}
}
