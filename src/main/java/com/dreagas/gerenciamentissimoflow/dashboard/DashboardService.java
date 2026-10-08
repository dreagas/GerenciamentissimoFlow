package com.dreagas.gerenciamentissimoflow.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.order.OrderStatus;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;

@Service
public class DashboardService {
	private final ProductRepository products;
	private final InventoryBalanceRepository balances;
	private final CustomerOrderRepository orders;
	private final InventoryMovementRepository movements;

	public DashboardService(ProductRepository products, InventoryBalanceRepository balances,
			CustomerOrderRepository orders, InventoryMovementRepository movements) {
		this.products = products;
		this.balances = balances;
		this.orders = orders;
		this.movements = movements;
	}

	@Transactional(readOnly = true)
	public DashboardSummary summary() {
		return new DashboardSummary(products.countByActiveTrue(), balances.countOutOfStockBalances(),
				balances.countLowStockBalances(), orders.countByStatus(OrderStatus.DRAFT),
				orders.countByStatus(OrderStatus.CONFIRMED), orders.countByStatus(OrderStatus.FULFILLED),
				movements.findTop10ByOrderByCreatedAtDesc().stream().map(RecentMovement::from).toList());
	}

	public record DashboardSummary(long activeProducts, long outOfStockBalances, long lowStockBalances,
			long draftOrders, long confirmedOrders, long fulfilledOrders, java.util.List<RecentMovement> recentMovements) { }

	public record RecentMovement(java.util.UUID id, String type, long quantity, String sku, String productName,
			String warehouseCode, java.time.Instant occurredAt) {
		static RecentMovement from(com.dreagas.gerenciamentissimoflow.inventory.InventoryMovement movement) {
			return new RecentMovement(movement.getId(), movement.getType(), movement.getQuantity(),
					movement.getProduct().getSku(), movement.getProduct().getName(), movement.getWarehouse().getCode(),
					movement.getCreatedAt());
		}
	}
}
