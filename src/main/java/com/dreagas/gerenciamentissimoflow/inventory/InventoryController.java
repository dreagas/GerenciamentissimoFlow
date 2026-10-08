package com.dreagas.gerenciamentissimoflow.inventory;

import java.util.UUID;
import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/receipts")
public class InventoryController {
	private final InventoryService service;
	private final InventoryBalanceRepository balances;
	private final InventoryMovementRepository movements;
	public InventoryController(InventoryService service, InventoryBalanceRepository balances, InventoryMovementRepository movements) {
		this.service = service; this.balances = balances; this.movements = movements;
	}

	@GetMapping("/movements")
	public Page<MovementResponse> movements(@RequestParam(required = false) UUID productId,
			@RequestParam(required = false) UUID warehouseId, @RequestParam(required = false) String type,
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("from must not be after to");
		Instant start = from == null ? Instant.parse("1970-01-01T00:00:00Z") : from;
		Instant end = to == null ? Instant.parse("9999-12-31T23:59:59Z") : to;
		return movements.search(productId, warehouseId, type, start, end, pageable).map(MovementResponse::from);
	}

	@GetMapping
	public Page<BalanceResponse> list(@RequestParam(required = false) UUID productId,
			@RequestParam(required = false) UUID warehouseId, @RequestParam(required = false) UUID categoryId,
			@RequestParam(defaultValue = "false") boolean lowStock,
			@RequestParam(defaultValue = "false") boolean outOfStock,
			@PageableDefault(size = 20, sort = "product.name") Pageable pageable) {
		return balances.search(productId, warehouseId, categoryId, lowStock, outOfStock, pageable).map(BalanceResponse::from);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReceiptResponse receive(@Valid @RequestBody ReceiptRequest request, Authentication authentication) {
		InventoryBalance balance = service.receive(request.productId(), request.warehouseId(), request.quantity(), request.reason(), authentication.getName());
		return new ReceiptResponse(balance.getId(), balance.getQuantity(), balance.getVersion());
	}

	@PutMapping("/adjustments")
	public ReceiptResponse adjust(@Valid @RequestBody AdjustmentRequest request, Authentication authentication) {
		InventoryBalance balance = service.adjust(request.productId(), request.warehouseId(), request.targetQuantity(), request.reason(), authentication.getName());
		return new ReceiptResponse(balance.getId(), balance.getQuantity(), balance.getVersion());
	}

	public record ReceiptRequest(@NotNull UUID productId, @NotNull UUID warehouseId,
			@Positive long quantity, @Size(max = 500) String reason) { }
	public record AdjustmentRequest(@NotNull UUID productId, @NotNull UUID warehouseId,
			@jakarta.validation.constraints.PositiveOrZero long targetQuantity,
			@NotNull @jakarta.validation.constraints.NotBlank @Size(max = 500) String reason) { }
	public record ReceiptResponse(UUID balanceId, long quantity, long version) { }
	public record BalanceResponse(UUID balanceId, UUID productId, String sku, String productName,
			UUID warehouseId, String warehouseCode, String warehouseName, long quantity, long minimumStock,
			boolean lowStock, boolean outOfStock) {
		static BalanceResponse from(InventoryBalance balance) {
			return new BalanceResponse(balance.getId(), balance.getProduct().getId(), balance.getProduct().getSku(),
					balance.getProduct().getName(), balance.getWarehouse().getId(), balance.getWarehouse().getCode(),
					balance.getWarehouse().getName(), balance.getQuantity(), balance.getProduct().getMinimumStock(),
					balance.getQuantity() <= balance.getProduct().getMinimumStock(), balance.getQuantity() == 0);
		}
	}
	public record MovementResponse(UUID id, UUID productId, String sku, String productName, UUID warehouseId,
			String warehouseCode, String warehouseName, String type, long quantity, String reason, Instant createdAt) {
		static MovementResponse from(InventoryMovement movement) {
			return new MovementResponse(movement.getId(), movement.getProduct().getId(), movement.getProduct().getSku(),
					movement.getProduct().getName(), movement.getWarehouse().getId(), movement.getWarehouse().getCode(),
					movement.getWarehouse().getName(), movement.getType(), movement.getQuantity(), movement.getReason(), movement.getCreatedAt());
		}
	}
}
