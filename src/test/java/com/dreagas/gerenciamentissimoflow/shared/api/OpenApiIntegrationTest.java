package com.dreagas.gerenciamentissimoflow.shared.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = { "springdoc.api-docs.enabled=true", "spring.web.cors.allowed-origins=http://localhost:3000" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OpenApiIntegrationTest {
	@Autowired private MockMvc mockMvc;
	@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void apiDescriptionIsPublicAndDeclaresBearerAuthentication() throws Exception {
		MvcResult response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.get("/v3/api-docs")).andReturn();
		assertEquals(HttpStatus.OK.value(), response.getResponse().getStatus());
		assertTrue(response.getResponse().getContentAsString().contains("bearerAuth"));
		assertTrue(response.getResponse().getContentAsString().contains("GerenciamentíssimoFlow API"));
	}
}
