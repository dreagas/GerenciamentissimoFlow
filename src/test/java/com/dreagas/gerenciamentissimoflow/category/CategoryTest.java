package com.dreagas.gerenciamentissimoflow.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CategoryTest {

	@Test
	void trimsNameAndCanBeRenamedAndDeactivated() {
		Category category = new Category("  Parts  ");

		assertEquals("parts", category.getName());
		category.rename(" Components ");
		category.deactivate();
		assertEquals("components", category.getName());
		assertFalse(category.isActive());
	}

	@Test
	void rejectsBlankName() {
		assertThrows(IllegalArgumentException.class, () -> new Category("  "));
	}
}
