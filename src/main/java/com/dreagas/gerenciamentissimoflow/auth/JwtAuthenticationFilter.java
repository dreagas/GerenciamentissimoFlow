package com.dreagas.gerenciamentissimoflow.auth;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final UserRepository users;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
		this.jwtService = jwtService;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null) {
			try {
				if (!header.startsWith("Bearer ") || header.length() <= 7) {
					throw new IllegalArgumentException("Invalid bearer token");
				}
				String email = jwtService.validateAndGetSubject(header.substring(7));
				var user = users.findByEmail(email).filter(User::isEnabled).orElse(null);
				if (user != null) {
					var authentication = new UsernamePasswordAuthenticationToken(email, null,
							java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
			catch (RuntimeException invalidToken) {
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}
}
