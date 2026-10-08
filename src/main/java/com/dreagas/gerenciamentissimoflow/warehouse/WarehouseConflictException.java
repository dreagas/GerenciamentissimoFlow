package com.dreagas.gerenciamentissimoflow.warehouse;

public class WarehouseConflictException extends RuntimeException {
	public WarehouseConflictException() { super("Warehouse code already exists"); }
}
