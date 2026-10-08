package com.dreagas.gerenciamentissimoflow.audit;

import java.util.UUID;

import com.dreagas.gerenciamentissimoflow.auth.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
	private final AuditEventRepository events;
	private final UserRepository users;

	public AuditService(AuditEventRepository events, UserRepository users) { this.events = events; this.users = users; }

	@Transactional
	public void record(String actorEmail, String action, String entityType, UUID entityId) {
		record(actorEmail, null, action, entityType, entityId);
	}

	@Transactional
	public void record(String actorEmail, UUID actorId, String action, String entityType, UUID entityId) {
		String normalizedEmail = actorEmail == null || actorEmail.isBlank() ? null : actorEmail.trim().toLowerCase(java.util.Locale.ROOT);
		UUID resolvedActorId = actorId;
		if (resolvedActorId == null && normalizedEmail != null)
			resolvedActorId = users.findByEmail(normalizedEmail).map(user -> user.getId()).orElse(null);
		events.save(new AuditEvent(resolvedActorId, normalizedEmail, action, entityType, entityId));
	}

	@Transactional(readOnly = true)
	public Page<AuditEvent> search(UUID actorId, String action, String entityType, Pageable pageable) {
		return events.search(actorId, normalize(action), normalize(entityType), pageable);
	}

	private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
