package com.dreagas.gerenciamentissimoflow.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
	private final OrderService service;
	public OrderController(OrderService service) { this.service = service; }

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse create(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
		return OrderResponse.from(service.create(request.warehouseId(), authentication.getName()));
	}

	@GetMapping("/{id}")
	public OrderResponse get(@PathVariable UUID id, Authentication authentication) {
		return OrderResponse.from(service.get(id, authentication.getName(), hasBroadRead(authentication)));
	}

	@GetMapping
	public Page<OrderResponse> list(Authentication authentication,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return service.list(authentication.getName(), hasBroadRead(authentication), pageable).map(OrderResponse::from);
	}

	@PostMapping("/{id}/items")
	public OrderResponse addItem(@PathVariable UUID id, @Valid @RequestBody ItemRequest request, Authentication authentication) {
		return OrderResponse.from(service.addItem(id, authentication.getName(), hasBroadRead(authentication), request.productId(), request.quantity(), request.unitPrice()));
	}

	@PutMapping("/{id}/items/{itemId}")
	public OrderResponse updateItem(@PathVariable UUID id, @PathVariable UUID itemId, @Valid @RequestBody UpdateItemRequest request, Authentication authentication) {
		return OrderResponse.from(service.updateItem(id, itemId, authentication.getName(), hasBroadRead(authentication), request.quantity(), request.unitPrice()));
	}

	@DeleteMapping("/{id}/items/{itemId}")
	public OrderResponse removeItem(@PathVariable UUID id, @PathVariable UUID itemId, Authentication authentication) {
		return OrderResponse.from(service.removeItem(id, itemId, authentication.getName(), hasBroadRead(authentication)));
	}

	@PostMapping("/{id}/confirm")
	public OrderResponse confirm(@PathVariable UUID id,
			@org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			Authentication authentication) {
		return OrderResponse.from(idempotencyKey == null
				? service.confirm(id, authentication.getName(), hasBroadRead(authentication))
				: service.confirm(id, authentication.getName(), hasBroadRead(authentication), idempotencyKey));
	}

	@PostMapping("/{id}/cancel")
	public OrderResponse cancel(@PathVariable UUID id, Authentication authentication) {
		return OrderResponse.from(service.cancel(id, authentication.getName(), hasBroadRead(authentication)));
	}

	@PostMapping("/{id}/fulfill")
	public OrderResponse fulfill(@PathVariable UUID id, Authentication authentication) {
		return OrderResponse.from(service.fulfill(id, authentication.getName(), hasBroadRead(authentication)));
	}

	private static boolean hasBroadRead(Authentication authentication) {
		return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
	}

	public record CreateOrderRequest(@NotNull UUID warehouseId) { }
	public record ItemRequest(@NotNull UUID productId, @Positive long quantity, @NotNull BigDecimal unitPrice) { }
	public record UpdateItemRequest(@Positive long quantity, @NotNull BigDecimal unitPrice) { }
	public record ItemResponse(UUID productId, String sku, String productName, long quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
		static ItemResponse from(OrderItem item) { return new ItemResponse(item.getProduct().getId(), item.getProduct().getSku(), item.getProduct().getName(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal()); }
	}
	public record OrderResponse(UUID id, OrderStatus status, UUID warehouseId, String warehouseCode, String warehouseName,
			UUID createdById, String createdByName, List<ItemResponse> items, BigDecimal totalAmount, Instant createdAt,
			Instant fulfilledAt, UUID fulfilledById) {
		static OrderResponse from(CustomerOrder order) {
			return new OrderResponse(order.getId(), order.getStatus(), order.getWarehouse().getId(), order.getWarehouse().getCode(),
					order.getWarehouse().getName(), order.getCreatedBy().getId(), order.getCreatedBy().getName(),
					order.getItems().stream().map(ItemResponse::from).toList(), order.getTotalAmount(), order.getCreatedAt(),
					order.getFulfilledAt(), order.getFulfilledBy() == null ? null : order.getFulfilledBy().getId());
		}
	}
}
