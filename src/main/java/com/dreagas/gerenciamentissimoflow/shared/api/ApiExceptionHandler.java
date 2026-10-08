package com.dreagas.gerenciamentissimoflow.shared.api;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.dreagas.gerenciamentissimoflow.category.CategoryConflictException;
import com.dreagas.gerenciamentissimoflow.category.CategoryNotFoundException;
import com.dreagas.gerenciamentissimoflow.product.ProductConflictException;
import com.dreagas.gerenciamentissimoflow.product.ProductNotFoundException;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierNotFoundException;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseConflictException;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseNotFoundException;
import com.dreagas.gerenciamentissimoflow.order.IdempotencyConflictException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error",
				"An unexpected error occurred.", "INTERNAL_ERROR");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", "Validation failed",
				"One or more request fields are invalid.", "VALIDATION_ERROR");
		problem.setProperty("fieldErrors", exception.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldError).toList());
		return problem;
	}

	@ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
	ProblemDetail handleResponseStatus(org.springframework.web.server.ResponseStatusException exception) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		return problem(status, status == HttpStatus.UNAUTHORIZED ? "unauthorized" : "request-error",
				status == HttpStatus.UNAUTHORIZED ? "Invalid credentials" : "Request rejected",
				status == HttpStatus.UNAUTHORIZED ? "Email or password is invalid." : "The request was rejected.",
				status == HttpStatus.UNAUTHORIZED ? "INVALID_CREDENTIALS" : "REQUEST_REJECTED");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleMalformedBody(HttpMessageNotReadableException exception) {
		return problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
				"The request body could not be read.", "MALFORMED_REQUEST");
	}

	@ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
	ProblemDetail handleNotFound(org.springframework.web.servlet.resource.NoResourceFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
				"The requested resource was not found.", "NOT_FOUND");
	}

	@ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
	ProblemDetail handleNoHandler(org.springframework.web.servlet.NoHandlerFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
				"The requested resource was not found.", "NOT_FOUND");
	}

	@ExceptionHandler(java.util.NoSuchElementException.class)
	ProblemDetail handleMissingResource(java.util.NoSuchElementException exception) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
				"The requested resource was not found.", "NOT_FOUND");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleInvalidInput(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request",
				"One or more request values are invalid.", "INVALID_REQUEST");
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
		return problem(HttpStatus.CONFLICT, "conflict", "Idempotency conflict",
				"The Idempotency-Key was already used for another operation.", "IDEMPOTENCY_KEY_CONFLICT");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		return problem(HttpStatus.CONFLICT, "conflict", "Resource conflict",
				"The request conflicts with an existing resource or database constraint.", "DATA_INTEGRITY_CONFLICT");
	}

	@ExceptionHandler(CategoryNotFoundException.class)
	ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
				"The requested resource was not found.", "NOT_FOUND");
	}

	@ExceptionHandler(CategoryConflictException.class)
	ProblemDetail handleCategoryConflict(CategoryConflictException exception) {
		return problem(HttpStatus.CONFLICT, "conflict", "Resource conflict",
				"A category with that name already exists.", "CATEGORY_NAME_CONFLICT");
	}

	@ExceptionHandler(ProductNotFoundException.class)
	ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
				"The requested resource was not found.", "NOT_FOUND");
	}

	@ExceptionHandler(ProductConflictException.class)
	ProblemDetail handleProductConflict(ProductConflictException exception) {
		return problem(HttpStatus.CONFLICT, "conflict", "Resource conflict",
				"A product with that SKU already exists.", "PRODUCT_SKU_CONFLICT");
	}

	@ExceptionHandler(SupplierNotFoundException.class)
	ProblemDetail handleSupplierNotFound(SupplierNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
				"The requested resource was not found.", "NOT_FOUND");
	}

	@ExceptionHandler(WarehouseNotFoundException.class)
	ProblemDetail handleWarehouseNotFound(WarehouseNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
				"The requested resource was not found.", "NOT_FOUND");
	}

	@ExceptionHandler(WarehouseConflictException.class)
	ProblemDetail handleWarehouseConflict(WarehouseConflictException exception) {
		return problem(HttpStatus.CONFLICT, "conflict", "Resource conflict",
				"A warehouse with that code already exists.", "WAREHOUSE_CODE_CONFLICT");
	}

	private Map<String, String> toFieldError(FieldError error) {
		return Map.of("field", error.getField(), "message", error.getDefaultMessage() == null
				? "Invalid value" : error.getDefaultMessage());
	}

	private ProblemDetail problem(HttpStatus status, String type, String title, String detail, String code) {
		ProblemDetail problem = ProblemDetail.forStatus(status);
		problem.setType(URI.create("https://gerenciamentissimoflow.invalid/problems/" + type));
		problem.setTitle(title);
		problem.setDetail(detail);
		problem.setProperty("code", code);
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}
}
