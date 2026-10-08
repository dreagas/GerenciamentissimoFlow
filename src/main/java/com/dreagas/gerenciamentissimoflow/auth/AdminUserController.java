package com.dreagas.gerenciamentissimoflow.auth;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import com.dreagas.gerenciamentissimoflow.audit.AuditService;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final UserRepository userRepository;
	private final AuditService audit;

	public AdminUserController(UserRepository userRepository, AuditService audit) {
		this.userRepository = userRepository;
		this.audit = audit;
	}

	@GetMapping
	@Transactional
	public List<AdminUserResponse> listUsers(Authentication authentication) {
		var actor = userRepository.findByEmail(authentication.getName()).orElse(null);
		audit.record(authentication.getName(), actor == null ? null : actor.getId(), "USER_ADMIN_LISTED", "USER", null);
		return userRepository.findAll().stream()
				.map(user -> new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()))
				.toList();
	}

	public record AdminUserResponse(UUID id, String name, String email, UserRole role) {
	}
}
