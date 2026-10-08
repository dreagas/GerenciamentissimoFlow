package com.dreagas.gerenciamentissimoflow.shared.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {
	@Bean
	OpenAPI gerenciamentissimoOpenApi() {
		String schemeName = "bearerAuth";
		return new OpenAPI()
				.info(new Info().title("GerenciamentíssimoFlow API")
						.description("API REST de gestão de estoque e pedidos B2B.")
						.version("v1"))
				.components(new Components().addSecuritySchemes(schemeName,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(schemeName));
	}
}
