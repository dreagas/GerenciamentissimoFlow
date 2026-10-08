package com.dreagas.gerenciamentissimoflow.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
	private final AuditService audit;

	public AuditController(AuditService audit) { this.audit = audit; }

	@GetMapping
	public Page<AuditEventResponse> list(@RequestParam(required = false) UUID actorId,
			@RequestParam(required = false) String action, @RequestParam(required = false) String entityType,
			@PageableDefault(size = 20, sort = "occurredAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return audit.search(actorId, action, entityType, pageable).map(AuditEventResponse::from);
	}

	public record AuditEventResponse(UUID id, UUID actorId, String actorEmail, String action,
			String entityType, UUID entityId, Instant occurredAt) {
		static AuditEventResponse from(AuditEvent event) {
			return new AuditEventResponse(event.getId(), event.getActorId(), event.getActorEmail(), event.getAction(),
					event.getEntityType(), event.getEntityId(), event.getOccurredAt());
		}
	}
}
