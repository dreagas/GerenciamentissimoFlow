package com.dreagas.gerenciamentissimoflow.shared.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.dreagas.gerenciamentissimoflow.audit.AuditEventRepository;
import com.dreagas.gerenciamentissimoflow.audit.AuditService;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.order.OrderConfirmationIdempotencyRepository;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"spring.web.cors.allowed-origins=http://localhost:3000",
		"app.jwt.secret=test-only-signing-key-with-at-least-32-bytes" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestCorrelationFilterTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private AuditEventRepository auditEvents;
	@MockitoBean
	private UserRepository users;
	@MockitoBean private AuditService auditService;
	@MockitoBean private CategoryRepository categories;
	@MockitoBean private ProductRepository products;
	@MockitoBean private SupplierRepository suppliers;
	@MockitoBean private WarehouseRepository warehouses;
	@MockitoBean private InventoryMovementRepository movements;
	@MockitoBean private InventoryBalanceRepository balances;
	@MockitoBean private CustomerOrderRepository orders;
	@MockitoBean private OrderConfirmationIdempotencyRepository idempotencies;

	@Test
	void assignsFreshServerGeneratedIdAndDoesNotTrustClientHeader() throws Exception {
		var first = mockMvc.perform(get("/actuator/health").header("X-Request-Id", "client-value")).andReturn();
		var second = mockMvc.perform(get("/actuator/health")).andReturn();
		String firstId = first.getResponse().getHeader(RequestCorrelationFilter.HEADER_NAME);
		String secondId = second.getResponse().getHeader(RequestCorrelationFilter.HEADER_NAME);

		assertEquals(HttpStatus.OK.value(), first.getResponse().getStatus());
		assertNotNull(firstId);
		assertEquals(UUID.fromString(firstId).toString(), firstId);
		assertNotNull(secondId);
		assertNotNull(UUID.fromString(secondId));
		assertFalse("client-value".equals(firstId));
		assertTrue(!firstId.equals(secondId));
		assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
	}
}
