package com.dreagas.gerenciamentissimoflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.dreagas.gerenciamentissimoflow.audit.AuditEventRepository;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.order.OrderConfirmationIdempotencyRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioLandingIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuditEventRepository auditEventRepository;

	@MockitoBean
	private UserRepository userRepository;
	@MockitoBean private CategoryRepository categoryRepository;
	@MockitoBean private ProductRepository productRepository;
	@MockitoBean private SupplierRepository supplierRepository;
	@MockitoBean private WarehouseRepository warehouseRepository;
	@MockitoBean private InventoryBalanceRepository inventoryBalanceRepository;
	@MockitoBean private InventoryMovementRepository inventoryMovementRepository;
	@MockitoBean private CustomerOrderRepository customerOrderRepository;
	@MockitoBean private OrderConfirmationIdempotencyRepository idempotencyRepository;

	@Test
	void landingPageIsPublicAndLinksApiHealthAndDocumentation() throws Exception {
		var result = mockMvc.perform(get("/")).andExpect(forwardedUrl("index.html")).andReturn();

		assertEquals(200, result.getResponse().getStatus());
		String html = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/static/index.html"));
		assertTrue(html.contains("Gerenciamentíssimo"));
		assertTrue(html.contains("/swagger-ui.html"));
		assertTrue(html.contains("/actuator/health"));
	}
}
