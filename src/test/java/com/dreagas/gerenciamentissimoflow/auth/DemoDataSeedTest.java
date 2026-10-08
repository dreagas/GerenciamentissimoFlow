package com.dreagas.gerenciamentissimoflow.auth;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dreagas.gerenciamentissimoflow.category.CategoryRepository;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryService;
import com.dreagas.gerenciamentissimoflow.inventory.InventoryBalanceRepository;
import com.dreagas.gerenciamentissimoflow.order.CustomerOrderRepository;
import com.dreagas.gerenciamentissimoflow.order.OrderService;
import com.dreagas.gerenciamentissimoflow.product.ProductRepository;
import com.dreagas.gerenciamentissimoflow.supplier.SupplierRepository;
import com.dreagas.gerenciamentissimoflow.warehouse.WarehouseRepository;

class DemoDataSeedTest {
    @Test
    void doesNothingWithoutDemoAdmin() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("demo-admin@example.invalid")).thenReturn(Optional.empty());
        DemoDataSeed seed = new DemoDataSeed(users, mock(CategoryRepository.class), mock(ProductRepository.class),
                mock(SupplierRepository.class), mock(WarehouseRepository.class), mock(CustomerOrderRepository.class),
                mock(InventoryService.class), mock(OrderService.class), "demo-admin@example.invalid", mock(InventoryBalanceRepository.class));

        seed.run();

        verify(users).findByEmail("demo-admin@example.invalid");
        verifyNoMoreInteractions(users);
    }

    @Test
    void safelySkipsBusinessScenarioWhenOrdersAlreadyExist() {
        UserRepository users = mock(UserRepository.class);
        User admin = new User("Demo Administrator", "demo-admin@example.invalid", "hash", UserRole.ADMIN);
        User manager = new User("Demo Manager", "demo-manager@example.invalid", "!demo-seed-account-disabled!", UserRole.MANAGER);
        when(users.findByEmail("demo-admin@example.invalid")).thenReturn(Optional.of(admin));
        when(users.findByEmail("demo-manager@example.invalid")).thenReturn(Optional.of(manager));
        when(users.findByEmail("demo-operator@example.invalid")).thenReturn(Optional.of(
                new User("Demo Operator", "demo-operator@example.invalid", "!demo-seed-account-disabled!", UserRole.OPERATOR)));
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        when(orders.findAll()).thenReturn(List.of(mock(com.dreagas.gerenciamentissimoflow.order.CustomerOrder.class)));
        InventoryService inventory = mock(InventoryService.class);
        DemoDataSeed seed = new DemoDataSeed(users, mock(CategoryRepository.class), mock(ProductRepository.class),
                mock(SupplierRepository.class), mock(WarehouseRepository.class), orders, inventory,
                mock(OrderService.class), "demo-admin@example.invalid", mock(InventoryBalanceRepository.class));

        seed.run();

        verifyNoInteractions(inventory);
    }
}
