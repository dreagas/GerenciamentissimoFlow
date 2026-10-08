package com.dreagas.gerenciamentissimoflow.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoAdminSeed implements CommandLineRunner {
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	public DemoAdminSeed(UserRepository users, PasswordEncoder passwordEncoder,
			@Value("${app.demo.admin.email}") String email,
			@Value("${app.demo.admin.password}") String password) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!StringUtils.hasText(password) || password.length() < 16) {
			throw new IllegalStateException("DEMO_ADMIN_PASSWORD must contain at least 16 characters");
		}
		seedAdmin();
	}

	private void seedAdmin() {
		users.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)).orElseGet(() -> users.save(
				new User("Demo Administrator", email, passwordEncoder.encode(password), UserRole.ADMIN)));
	}
}
