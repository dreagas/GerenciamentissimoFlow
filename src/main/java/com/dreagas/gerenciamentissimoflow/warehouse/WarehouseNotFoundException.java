package com.dreagas.gerenciamentissimoflow.warehouse;

import java.util.UUID;

public class WarehouseNotFoundException extends RuntimeException {
	private final UUID id;

	public WarehouseNotFoundException(UUID id) {
		super("Warehouse not found");
		this.id = id;
	}

	public UUID getId() { return id; }
}
