package com.dreagas.gerenciamentissimoflow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.audit.AuditEventRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryMovementRepository;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"management.health.db.enabled=false"
})
class GerenciamentissimoflowApplicationTests {
	@Autowired
	private Environment environment;
	@MockitoBean
	private UserRepository userRepository;
	@MockitoBean
	private CategoryRepository categoryRepository;
	@MockitoBean
	private ProductRepository productRepository;
	@MockitoBean
	private SupplierRepository supplierRepository;
	@MockitoBean
	private WarehouseRepository warehouseRepository;
	@MockitoBean private InventoryBalanceRepository inventoryBalanceRepository;
	@MockitoBean private InventoryMovementRepository inventoryMovementRepository;
	@MockitoBean private CustomerOrderRepository customerOrderRepository;
	@MockitoBean private com.dreagas.gerenciamentissimoflow.order.OrderConfirmationIdempotencyRepository orderConfirmationIdempotencyRepository;
	@MockitoBean private AuditEventRepository auditEventRepository;

	@Test
	void contextLoads() {
		assertTrue(environment.acceptsProfiles(org.springframework.core.env.Profiles.of("test")));
	}

}
