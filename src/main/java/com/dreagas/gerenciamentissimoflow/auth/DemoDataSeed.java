package com.dreagas.gerenciamentissimoflow.auth;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dreagas.gerenciamentissimoflow.category.Category;
import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryService;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrder;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.order.OrderService;
import com.dreagas.gerenciamentissimoflow.order.OrderItem;
import com.dreagas.gerenciamentissimoflow.order.OrderStatus;
import com.dreagas.gerenciamentissimoflow.product.Product;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.supplier.Supplier;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.Warehouse;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

@Component
@Profile("demo")
public class DemoDataSeed implements CommandLineRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final WarehouseRepository warehouses;
    private final CustomerOrderRepository orders;
    private final InventoryService inventory;
    private final OrderService orderService;
    private final String adminEmail;
    private final InventoryBalanceRepository balances;

    public DemoDataSeed(UserRepository users, CategoryRepository categories, ProductRepository products,
            SupplierRepository suppliers, WarehouseRepository warehouses, CustomerOrderRepository orders,
            InventoryService inventory, OrderService orderService,
            @Value("${app.demo.admin.email}") String adminEmail, InventoryBalanceRepository balances) {
        this.users = users;
        this.categories = categories;
        this.products = products;
        this.suppliers = suppliers;
        this.warehouses = warehouses;
        this.orders = orders;
        this.inventory = inventory;
        this.orderService = orderService;
        this.adminEmail = adminEmail;
        this.balances = balances;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = users.findByEmail(adminEmail.trim().toLowerCase(java.util.Locale.ROOT)).orElse(null);
        if (admin == null) return;
        User manager = seedUser("demo-manager@example.invalid", "Demo Manager", UserRole.MANAGER);
        seedUser("demo-operator@example.invalid", "Demo Operator", UserRole.OPERATOR);

        Category category = categories.findAll().stream()
                .filter(value -> value.getName().equalsIgnoreCase("demo components")).findFirst()
                .orElseGet(() -> categories.save(new Category("Demo Components")));
        Supplier supplier = suppliers.findAll().stream()
                .filter(value -> value.getName().equalsIgnoreCase("Demo Supply Co.")).findFirst()
                .orElseGet(() -> suppliers.save(new Supplier("Demo Supply Co.", null, "supplier@example.invalid", null)));
        Warehouse warehouse = warehouses.findAll().stream()
                .filter(value -> value.getCode().equalsIgnoreCase("DEMO-WH")).findFirst()
                .orElseGet(() -> warehouses.save(new Warehouse("DEMO-WH", "Demo Central Warehouse", "Demo City")));
        Product product = products.findAll().stream()
                .filter(value -> value.getSku().equalsIgnoreCase("DEMO-COMP-001")).findFirst()
                .orElseGet(() -> products.save(new Product("DEMO-COMP-001", "Demo Component", "Fictional inventory item",
                        category, BigDecimal.valueOf(12.50), 5)));

        if (orders.findAll().isEmpty() && balances.lockByProductAndWarehouse(product.getId(), warehouse.getId()).isEmpty()) {
            inventory.receive(product.getId(), warehouse.getId(), 40, "Initial deterministic demo stock", admin.getEmail());
        }
    }

    private User seedUser(String email, String name, UserRole role) {
        User user = users.findByEmail(email).orElse(null);
        if (user != null) return user;
        return users.save(new User(name, email, "!demo-seed-account-disabled!", role));
    }
}
