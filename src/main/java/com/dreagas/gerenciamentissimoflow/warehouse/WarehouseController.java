package com.dreagas.gerenciamentissimoflow.warehouse;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {
	private final WarehouseService service;

	public WarehouseController(WarehouseService service) { this.service = service; }

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WarehouseResponse create(@Valid @RequestBody WarehouseRequest request, Authentication authentication) {
		return WarehouseResponse.from(service.create(request.code(), request.name(), request.location(), authentication.getName()));
	}

	@GetMapping
	public Page<WarehouseResponse> list(@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) String q,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.list(active, q, pageable).map(WarehouseResponse::from);
	}

	@GetMapping("/{id}")
	public WarehouseResponse get(@PathVariable UUID id) { return WarehouseResponse.from(service.get(id)); }

	@PutMapping("/{id}")
	public WarehouseResponse update(@PathVariable UUID id, @Valid @RequestBody WarehouseRequest request, Authentication authentication) {
		return WarehouseResponse.from(service.update(id, request.code(), request.name(), request.location(), authentication.getName()));
	}

	@DeleteMapping("/{id}")
	public WarehouseResponse deactivate(@PathVariable UUID id, Authentication authentication) {
		return WarehouseResponse.from(service.deactivate(id, authentication.getName()));
	}

	public record WarehouseRequest(@NotBlank @Size(max = 40) String code,
			@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 240) String location) { }

	public record WarehouseResponse(UUID id, String code, String name, String location, boolean active,
			Instant createdAt, Instant updatedAt) {
		static WarehouseResponse from(Warehouse warehouse) {
			return new WarehouseResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName(),
					warehouse.getLocation(), warehouse.isActive(), warehouse.getCreatedAt(), warehouse.getUpdatedAt());
		}
	}
}
