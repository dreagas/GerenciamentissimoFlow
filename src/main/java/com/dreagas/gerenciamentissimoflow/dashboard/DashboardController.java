package com.dreagas.gerenciamentissimoflow.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
	private final DashboardService service;
	public DashboardController(DashboardService service) { this.service = service; }

	@GetMapping
	public DashboardService.DashboardSummary summary() { return service.summary(); }
}
