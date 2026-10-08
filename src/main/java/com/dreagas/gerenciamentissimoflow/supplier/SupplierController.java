package com.dreagas.gerenciamentissimoflow.supplier;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
	private final SupplierService service;
	public SupplierController(SupplierService service) { this.service = service; }
	@PostMapping @ResponseStatus(HttpStatus.CREATED)
	public SupplierResponse create(@Valid @RequestBody SupplierRequest request, Authentication authentication) {
		return SupplierResponse.from(service.create(request.name(), request.document(), request.email(), request.phone(), authentication.getName()));
	}
	@GetMapping
	public Page<SupplierResponse> list(@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.search(q, active, pageable).map(SupplierResponse::from);
	}
	@GetMapping("/{id}") public SupplierResponse get(@PathVariable UUID id) { return SupplierResponse.from(service.get(id)); }
	@PutMapping("/{id}")
	public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request, Authentication authentication) {
		return SupplierResponse.from(service.update(id, request.name(), request.document(), request.email(), request.phone(), authentication.getName()));
	}
	@DeleteMapping("/{id}") public SupplierResponse deactivate(@PathVariable UUID id, Authentication authentication) { return SupplierResponse.from(service.deactivate(id, authentication.getName())); }

	public record SupplierRequest(@NotBlank @Size(max = 180) String name,
			@Size(max = 40) String document, @Email @Size(max = 320) String email, @Size(max = 40) String phone) { }
	public record SupplierResponse(UUID id, String name, String document, String email, String phone, boolean active,
			Instant createdAt, Instant updatedAt) {
		static SupplierResponse from(Supplier supplier) {
			return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getDocument(), supplier.getEmail(),
					supplier.getPhone(), supplier.isActive(), supplier.getCreatedAt(), supplier.getUpdatedAt());
		}
	}
}
