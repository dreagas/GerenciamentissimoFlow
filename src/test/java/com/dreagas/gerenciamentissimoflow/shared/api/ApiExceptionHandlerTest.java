package com.dreagas.gerenciamentissimoflow.shared.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void unexpectedFailuresNeverExposeInternalExceptionMessage() {
		ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("password=do-not-leak"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
		assertEquals("INTERNAL_ERROR", problem.getProperties().get("code"));
		assertEquals("An unexpected error occurred.", problem.getDetail());
		assertNotNull(problem.getProperties().get("timestamp"));
	}

	@Test
	void databaseConstraintFailuresBecomeSanitizedConflicts() {
		ProblemDetail problem = handler.handleDataIntegrityViolation(
				new DataIntegrityViolationException("duplicate key password=do-not-leak"));

		assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
		assertEquals("DATA_INTEGRITY_CONFLICT", problem.getProperties().get("code"));
		assertEquals("The request conflicts with an existing resource or database constraint.", problem.getDetail());
	}
}
