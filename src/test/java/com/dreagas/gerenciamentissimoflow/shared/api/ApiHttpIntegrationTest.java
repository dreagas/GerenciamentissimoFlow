package com.dreagas.gerenciamentissimoflow.shared.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.ActiveProfiles;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.category.CategoryController;
import com.dreagas.gerenciamentissimoflow.category.CategoryService;
import com.dreagas.gerenciamentissimoflow.product.ProductController;
import com.dreagas.gerenciamentissimoflow.product.ProductService;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierController;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierRepository;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierService;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseController;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseService;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryController;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryService;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.order.OrderController;
import com.dreagas.gerenciamentissimoflow.order.OrderService;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.auth.CurrentUserController;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.dreagas.gerenciamentissimoflow.auth.User;
import com.dreagas.gerenciamentissimoflow.auth.UserRole;
import com.dreagas.gerenciamentissimoflow.audit.AuditController;
import com.dreagas.gerenciamentissimoflow.audit.AuditEventRepository;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest(
		properties = {
				"spring.autoconfigure.exclude=org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
				"spring.web.cors.allowed-origins=http://localhost:3000",
				"app.jwt.secret=test-only-signing-key-with-at-least-32-bytes"
		})
@AutoConfigureMockMvc
@ActiveProfiles("test")
	@Import({ApiHttpIntegrationTest.TestJsonController.class, CurrentUserController.class, CategoryController.class,
			CategoryService.class, ProductController.class, ProductService.class, SupplierController.class, SupplierService.class,
			WarehouseController.class, WarehouseService.class, InventoryController.class, InventoryService.class,
			OrderController.class, OrderService.class, AuditController.class, AuditService.class})
class ApiHttpIntegrationTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired private PasswordEncoder passwordEncoder;

	@MockitoBean
	private UserRepository userRepository;
	@MockitoBean
	private AuditEventRepository auditEventRepository;
	@MockitoBean
	private AuditService auditService;

	@MockitoBean
	private CategoryRepository categoryRepository;

	@MockitoBean
	private ProductRepository productRepository;

	@MockitoBean
	private SupplierRepository supplierRepository;
	@MockitoBean
	private WarehouseRepository warehouseRepository;
	@MockitoBean private CustomerOrderRepository customerOrderRepository;
	@MockitoBean private com.dreagas.gerenciamentissimoflow.order.OrderConfirmationIdempotencyRepository orderConfirmationIdempotencyRepository;
	@MockitoBean private InventoryMovementRepository inventoryMovementRepository;
	@MockitoBean private InventoryBalanceRepository inventoryBalanceRepository;

	@Test
	void unknownRouteReturnsSafeProblemDetail() throws Exception {
		MvcResult response = mockMvc.perform(get("/missing-route").with(user("http-test-user"))).andReturn();

		assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus());
		assertTrue(response.getResponse().getContentAsString().contains("NOT_FOUND"));
		assertTrue(response.getResponse().getContentAsString().contains("not-found"));
	}

	@Test
	void malformedJsonReturnsSafeBadRequest() throws Exception {
		MvcResult response = mockMvc.perform(post("/api/v1/test-json").with(user("http-test-user"))
				.with(csrf()).contentType("application/json").content("{invalid")).andReturn();

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getResponse().getStatus());
		assertTrue(response.getResponse().getContentAsString().contains("MALFORMED_REQUEST"));
	}

	@Test
	void unsafeRequestWithoutCsrfIsForbiddenEvenWhenAuthenticated() throws Exception {
		MvcResult response = mockMvc.perform(post("/api/v1/test-json").with(user("http-test-user"))
				.contentType("application/json").content("{}"))
				.andReturn();

		assertEquals(HttpStatus.FORBIDDEN.value(), response.getResponse().getStatus());
	}

	@Test
	void adminEndpointAllowsAdminAndRejectsOtherRolesAndAnonymousRequests() throws Exception {
		assertEquals(HttpStatus.OK.value(), mockMvc.perform(get("/api/v1/admin/users")
				.with(user("admin").roles("ADMIN"))).andReturn().getResponse().getStatus());
		assertEquals(HttpStatus.FORBIDDEN.value(), mockMvc.perform(get("/api/v1/admin/users")
				.with(user("manager").roles("MANAGER"))).andReturn().getResponse().getStatus());
		assertEquals(HttpStatus.FORBIDDEN.value(), mockMvc.perform(get("/api/v1/admin/users")
				.with(user("operator").roles("OPERATOR"))).andReturn().getResponse().getStatus());
		MvcResult anonymous = mockMvc.perform(get("/api/v1/admin/users")).andReturn();
		assertEquals(HttpStatus.UNAUTHORIZED.value(), anonymous.getResponse().getStatus());
		assertTrue(anonymous.getResponse().getContentAsString().contains("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void loginIsPublicAndInvalidCredentialsReturnUnauthorized() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"missing@example.com\",\"password\":\"invalid\"}")).andReturn();
		assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getResponse().getStatus());
		assertTrue(!result.getResponse().getContentAsString().contains("missing@example.com"));
	}

	@Test
	void loginTokenAuthenticatesAndDisabledUsersAreRejected() throws Exception {
		User user = new User("JWT User", "jwt@example.com", passwordEncoder.encode("valid-password"), UserRole.MANAGER);
		when(userRepository.findByEmail("jwt@example.com")).thenReturn(Optional.of(user));
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"jwt@example.com\",\"password\":\"valid-password\"}")).andReturn();
		assertEquals(HttpStatus.OK.value(), login.getResponse().getStatus());
		JsonNode response = new com.fasterxml.jackson.databind.ObjectMapper()
				.readTree(login.getResponse().getContentAsString());
		String token = response.get("accessToken").asText();
		assertEquals(HttpStatus.OK.value(), mockMvc.perform(get("/api/v1/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse().getStatus());
		assertEquals(HttpStatus.UNAUTHORIZED.value(), mockMvc.perform(get("/api/v1/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.substring(0, token.length() - 2) + "aa"))
				.andReturn().getResponse().getStatus());
		assertEquals(HttpStatus.UNAUTHORIZED.value(), mockMvc.perform(get("/api/v1/me")
				.header(HttpHeaders.AUTHORIZATION, "Basic not-a-token")).andReturn().getResponse().getStatus());
		user.disable();
		assertEquals(HttpStatus.UNAUTHORIZED.value(), mockMvc.perform(get("/api/v1/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse().getStatus());
	}

	@Test
	void auditEndpointIsAdminOnly() throws Exception {
		when(auditService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(org.springframework.data.domain.Page.empty());
		assertEquals(HttpStatus.OK.value(), mockMvc.perform(get("/api/v1/audit-events")
				.with(user("admin").roles("ADMIN"))).andReturn().getResponse().getStatus());
		assertEquals(HttpStatus.FORBIDDEN.value(), mockMvc.perform(get("/api/v1/audit-events")
				.with(user("manager").roles("MANAGER"))).andReturn().getResponse().getStatus());
		assertEquals(HttpStatus.UNAUTHORIZED.value(), mockMvc.perform(get("/api/v1/audit-events"))
				.andReturn().getResponse().getStatus());
	}

	@Test
	void currentUserEndpointReturnsOnlyTheAuthenticatedUsersPublicFields() throws Exception {
		when(userRepository.findByEmail("alice@example.com"))
				.thenReturn(Optional.of(new User("Alice", "alice@example.com", "never-return-this", UserRole.MANAGER)));

		MvcResult response = mockMvc.perform(get("/api/v1/me").with(user("alice@example.com").roles("MANAGER")))
				.andReturn();

		assertEquals(HttpStatus.OK.value(), response.getResponse().getStatus());
		assertTrue(response.getResponse().getContentAsString().contains("alice@example.com"));
		assertTrue(response.getResponse().getContentAsString().contains("MANAGER"));
		assertTrue(!response.getResponse().getContentAsString().contains("never-return-this"));
	}

	@Test
	void corsPreflightAllowsConfiguredOriginAndRequestedOperations() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://localhost:3000");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-csrf-token");
		MvcResult response = mockMvc.perform(options("/api/v1/test-json").headers(headers)
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")).andReturn();

		assertEquals(HttpStatus.OK.value(), response.getResponse().getStatus());
		assertEquals("http://localhost:3000", response.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS).contains("POST"));
		assertTrue(response.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).toLowerCase()
				.contains("content-type"));
	}

	@Test
	void corsPreflightDoesNotAllowUnconfiguredOrigin() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "https://untrusted.example");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		MvcResult response = mockMvc.perform(options("/actuator/health").headers(headers)
				.header(HttpHeaders.ORIGIN, "https://untrusted.example")).andReturn();

		assertNull(response.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void operatorCanCallDraftOrderItemRoutes() throws Exception {
		var productId = java.util.UUID.randomUUID();
		String payload = "{\"productId\":\"" + productId + "\",\"quantity\":2,\"unitPrice\":\"3.50\"}";
		assertEquals(HttpStatus.BAD_REQUEST.value(), mockMvc.perform(post("/api/v1/orders/" + java.util.UUID.randomUUID() + "/items")
				.with(user("operator").roles("OPERATOR")).with(csrf()).contentType("application/json").content(payload)).andReturn().getResponse().getStatus());
	}

	@RestController
	static class TestJsonController {
		@PostMapping(path = "/api/v1/test-json", consumes = "application/json")
		void acceptJson(@RequestBody TestPayload payload) {
		}
	}

	record TestPayload(String value) {
	}
}
