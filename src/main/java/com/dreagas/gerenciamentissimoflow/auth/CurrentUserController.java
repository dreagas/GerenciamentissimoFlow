package com.dreagas.gerenciamentissimoflow.auth;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.dreagas.gerenciamentissimoflow.audit.AuditService;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentUserController {

	private final UserRepository userRepository;
	private final AuditService audit;

	public CurrentUserController(UserRepository userRepository, AuditService audit) {
		this.userRepository = userRepository;
		this.audit = audit;
	}

	@GetMapping
	@Transactional
	public CurrentUserResponse getCurrentUser(Authentication authentication) {
		User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
		audit.record(user.getEmail(), user.getId(), "USER_LOGIN", "USER", user.getId());
		return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
	}

	public record CurrentUserResponse(UUID id, String name, String email, UserRole role) {
	}
}
