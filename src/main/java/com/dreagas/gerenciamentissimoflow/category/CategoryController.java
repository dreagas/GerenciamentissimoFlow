package com.dreagas.gerenciamentissimoflow.category;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryService service;

	public CategoryController(CategoryService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryResponse create(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
		return CategoryResponse.from(service.create(request.name(), authentication.getName()));
	}

	@GetMapping
	public Page<CategoryResponse> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.list(pageable).map(CategoryResponse::from);
	}

	@GetMapping(params = {"q", "active"})
	public Page<CategoryResponse> searchByNameAndStatus(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String q,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.search(q, active, pageable).map(CategoryResponse::from);
	}

	@GetMapping(params = "q")
	public Page<CategoryResponse> searchByName(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String q,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.search(q, null, pageable).map(CategoryResponse::from);
	}

	@GetMapping(params = "active")
	public Page<CategoryResponse> searchByStatus(
			@org.springframework.web.bind.annotation.RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return service.search(null, active, pageable).map(CategoryResponse::from);
	}

	@GetMapping("/{id}")
	public CategoryResponse get(@PathVariable UUID id) {
		return CategoryResponse.from(service.get(id));
	}

	@PutMapping("/{id}")
	public CategoryResponse rename(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request, Authentication authentication) {
		return CategoryResponse.from(service.rename(id, request.name(), authentication.getName()));
	}

	@DeleteMapping("/{id}")
	public CategoryResponse deactivate(@PathVariable UUID id, Authentication authentication) {
		return CategoryResponse.from(service.deactivate(id, authentication.getName()));
	}

	public record CategoryRequest(@NotBlank @Size(max = 120) String name) {
	}

	public record CategoryResponse(UUID id, String name, boolean active, Instant createdAt, Instant updatedAt) {
		static CategoryResponse from(Category category) {
			return new CategoryResponse(category.getId(), category.getName(), category.isActive(),
					category.getCreatedAt(), category.getUpdatedAt());
		}
	}
}
