package com.dreagas.gerenciamentissimoflow.shared.api;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.dreagas.gerenciamentissimoflow.auth.JwtAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
class ApiSecurityConfiguration {

	@Bean
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
			JwtAuthenticationFilter jwtAuthenticationFilter)
			throws Exception {
		var objectMapper = new ObjectMapper().findAndRegisterModules();
		http.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/auth/login"))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) -> writeSecurityProblem(
								response, HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication required",
								"AUTHENTICATION_REQUIRED", objectMapper))
						.accessDeniedHandler((request, response, exception) -> writeSecurityProblem(
								response, HttpStatus.FORBIDDEN, "forbidden", "Access denied",
								"ACCESS_DENIED", objectMapper)))
				.authorizeHttpRequests(authorize -> authorize
					.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
					.requestMatchers("/", "/index.html", "/styles.css").permitAll()
					.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
					.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
					.requestMatchers("/api/v1/audit-events/**").hasRole("ADMIN")
					.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
					.requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**", "/api/v1/products", "/api/v1/products/**")
						.hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers("/api/v1/categories/**", "/api/v1/products/**")
						.hasAnyRole("ADMIN", "MANAGER")
					.requestMatchers(HttpMethod.GET, "/api/v1/suppliers", "/api/v1/suppliers/**")
						.hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers("/api/v1/suppliers/**").hasAnyRole("ADMIN", "MANAGER")
					.requestMatchers(HttpMethod.GET, "/api/v1/warehouses", "/api/v1/warehouses/**")
						.hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers("/api/v1/warehouses/**").hasAnyRole("ADMIN", "MANAGER")
					.requestMatchers(HttpMethod.POST, "/api/v1/inventory/**").hasAnyRole("ADMIN", "MANAGER")
					.requestMatchers(HttpMethod.PUT, "/api/v1/inventory/**").hasRole("ADMIN")
					.requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyRole("ADMIN", "MANAGER")
					.requestMatchers(HttpMethod.GET, "/api/v1/orders", "/api/v1/orders/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers(HttpMethod.POST, "/api/v1/orders/*/items").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers(HttpMethod.POST, "/api/v1/orders/*/confirm").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
					.anyRequest().authenticated())
				.userDetailsService(username -> org.springframework.security.core.userdetails.User
						.withUsername(username).password("").authorities("ROLE_USER").build());
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
		return username -> org.springframework.security.core.userdetails.User.withUsername(username)
				.password("{noop}disabled")
				.authorities("ROLE_USER")
				.build();
	}

    private static void writeSecurityProblem(jakarta.servlet.http.HttpServletResponse response, HttpStatus status,
            String type, String title, String code, ObjectMapper objectMapper) throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("https://gerenciamentissimoflow.invalid/problems/" + type));
        problem.setTitle(title);
        problem.setDetail(status == HttpStatus.UNAUTHORIZED
                ? "Authentication is required to access this resource."
                : "You are not allowed to access this resource.");
        problem.setProperty("code", code);
		problem.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${spring.web.cors.allowed-origins}") String configuredOrigins) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(Arrays.stream(configuredOrigins.split(","))
				.map(String::trim).filter(origin -> !origin.isEmpty()).toList());
		cors.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-CSRF-TOKEN"));
		cors.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return source;
	}
}
