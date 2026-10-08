package com.dreagas.gerenciamentissimoflow.inventory;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/reports/inventory")
public class InventoryReportController {
	private final InventoryCsvExporter exporter;
	public InventoryReportController(InventoryCsvExporter exporter) { this.exporter = exporter; }

	@GetMapping(value = "/stock-position.csv", produces = "text/csv")
	public void stockPosition(@RequestParam(required = false) UUID warehouseId,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(defaultValue = "false") boolean lowStock,
			HttpServletResponse response) throws IOException {
		response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
		response.setContentType("text/csv;charset=UTF-8");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-stock-position.csv");
		exporter.write(warehouseId, categoryId, lowStock, response.getWriter());
	}
}
