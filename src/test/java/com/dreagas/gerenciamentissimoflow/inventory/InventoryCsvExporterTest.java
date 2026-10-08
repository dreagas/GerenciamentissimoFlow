package com.dreagas.gerenciamentissimoflow.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class InventoryCsvExporterTest {
	@Test
	void escapesCsvQuotesAndNeutralizesFormulaPrefixes() throws Exception {
		StringWriter writer = new StringWriter();
		InventoryCsvExporter.writeCell(writer, "=1+1,\"value\"");
		assertEquals("\"'=1+1,\"\"value\"\"\"", writer.toString());
	}
}
