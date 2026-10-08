package com.dreagas.gerenciamentissimoflow.inventory;

import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCsvExporter {
	private static final int PAGE_SIZE = 500;
	private final InventoryBalanceRepository balances;

	public InventoryCsvExporter(InventoryBalanceRepository balances) { this.balances = balances; }

	@Transactional(readOnly = true)
	public void write(UUID warehouseId, UUID categoryId, boolean lowStock, Writer writer) throws IOException {
		writer.write("sku,product_name,warehouse_code,quantity,minimum_stock,low_stock\r\n");
		int page = 0;
		while (true) {
			var rows = balances.search(null, warehouseId, categoryId, lowStock, false,
					PageRequest.of(page++, PAGE_SIZE, Sort.by(Sort.Order.asc("product.sku"), Sort.Order.asc("warehouse.code"))));
			for (InventoryBalance balance : rows) {
				writeCell(writer, balance.getProduct().getSku()); writer.write(',');
				writeCell(writer, balance.getProduct().getName()); writer.write(',');
				writeCell(writer, balance.getWarehouse().getCode()); writer.write(',');
				writer.write(Long.toString(balance.getQuantity())); writer.write(',');
				writer.write(Long.toString(balance.getProduct().getMinimumStock())); writer.write(',');
				writer.write(Boolean.toString(balance.getQuantity() <= balance.getProduct().getMinimumStock()));
				writer.write("\r\n");
			}
			if (!rows.hasNext()) break;
		}
	}

	static void writeCell(Writer writer, String raw) throws IOException {
		String value = raw == null ? "" : raw;
		if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
		writer.write('"');
		writer.write(value.replace("\"", "\"\""));
		writer.write('"');
	}
}
