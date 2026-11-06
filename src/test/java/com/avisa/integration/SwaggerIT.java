package com.avisa.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La documentació de l'API (Swagger) només existeix amb el perfil dev.
 */
class SwaggerIT {

	@Nested
	@AutoConfigureMockMvc
	@DisplayName("Sense perfil (producció)")
	class Produccio extends PostgresIntegrationTest {

		@Autowired
		private MockMvc mvc;

		@Test
		@DisplayName("Ni /v3/api-docs ni /swagger-ui es poden veure sense ser dev")
		void noHiHaSwagger() throws Exception {
			mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
			mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
			mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@AutoConfigureMockMvc
	@ActiveProfiles("dev")
	@DisplayName("Amb el perfil dev")
	class Desenvolupament extends PostgresIntegrationTest {

		@Autowired
		private MockMvc mvc;

		@Test
		@DisplayName("/v3/api-docs respon")
		void hiHaSwagger() throws Exception {
			mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
			mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		}
	}
}
