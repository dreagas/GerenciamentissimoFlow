package com.dreagas.gerenciamentissimoflow.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
	private final ProductService service;

	public ProductController(ProductService service) { this.service = service; }

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse create(@Valid @RequestBody ProductRequest request, Authentication authentication) {
		return ProductResponse.from(service.create(request.toData(), authentication.getName()));
	}

	@GetMapping
	public Page<ProductResponse> search(@RequestParam(required = false) String q,
			@RequestParam(required = false) UUID categoryId, @RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.search(q, categoryId, active, pageable).map(ProductResponse::from);
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable UUID id) { return ProductResponse.from(service.get(id)); }

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request, Authentication authentication) {
		return ProductResponse.from(service.update(id, request.toData(), authentication.getName()));
	}

	@DeleteMapping("/{id}")
	public ProductResponse deactivate(@PathVariable UUID id, Authentication authentication) { return ProductResponse.from(service.deactivate(id, authentication.getName())); }

	public record ProductRequest(@NotBlank @Size(max = 80) String sku,
			@NotBlank @Size(max = 180) String name, @Size(max = 5000) String description,
			UUID categoryId, @NotNull @DecimalMin("0.0") @Digits(integer = 15, fraction = 4) BigDecimal referencePrice,
			@PositiveOrZero long minimumStock) {
		ProductService.ProductData toData() {
			return new ProductService.ProductData(sku, name, description, categoryId, referencePrice, minimumStock);
		}
	}

	public record ProductResponse(UUID id, String sku, String name, String description, UUID categoryId,
			BigDecimal referencePrice, long minimumStock, boolean active, Instant createdAt, Instant updatedAt) {
		static ProductResponse from(Product product) {
			return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
					product.getCategory() == null ? null : product.getCategory().getId(), product.getReferencePrice(),
					product.getMinimumStock(), product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
		}
	}
}
