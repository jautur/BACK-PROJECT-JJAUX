package com.avisa.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avisa.push.ServeiPush;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PushControllerTest {

	private static final String CORS = "{\"endpoint\":\"https://fcm.googleapis.com/x\",\"keys\":{\"p256dh\":\"k\",\"auth\":\"a\"}}";

	private MockMvc mvc;

	@Mock
	private ServeiPush servei;

	@InjectMocks
	private PushController controlador;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new com.avisa.config.GlobalExceptionHandler()).build();
	}

	@Test
	@DisplayName("La clau pública: 404 si el push no està configurat, i la clau si ho està")
	void clauPublica() throws Exception {
		when(servei.activat()).thenReturn(false);
		mvc.perform(get("/api/push/clau-publica")).andExpect(status().isNotFound());

		when(servei.activat()).thenReturn(true);
		when(servei.clauPublica()).thenReturn("BCLAU");
		mvc.perform(get("/api/push/clau-publica")).andExpect(status().isOk()).andExpect(jsonPath("$.clau").value("BCLAU"));
	}

	@Test
	@DisplayName("Subscriure: sense sessió 401, amb el push desactivat 503, amb dades dolentes 400, correcte 204")
	void subscriure() throws Exception {
		mvc.perform(post("/api/push/subscripcions").contentType(MediaType.APPLICATION_JSON).content(CORS)).andExpect(status().isUnauthorized());

		when(servei.activat()).thenReturn(false);
		mvc.perform(post("/api/push/subscripcions").principal(JwtDeProva.sessio("u-1", "CLIENT")).contentType(MediaType.APPLICATION_JSON).content(CORS))
			.andExpect(status().isServiceUnavailable());

		when(servei.activat()).thenReturn(true);
		when(servei.subscriure("u-1", "https://fcm.googleapis.com/x", "k", "a")).thenReturn(true);
		mvc.perform(post("/api/push/subscripcions").principal(JwtDeProva.sessio("u-1", "CLIENT")).contentType(MediaType.APPLICATION_JSON).content(CORS))
			.andExpect(status().isNoContent());

		mvc.perform(post("/api/push/subscripcions").principal(JwtDeProva.sessio("u-1", "CLIENT")).contentType(MediaType.APPLICATION_JSON).content("{\"endpoint\":\"https://evil.example/x\",\"keys\":{\"p256dh\":\"k\",\"auth\":\"a\"}}"))
			.andExpect(status().isBadRequest());
		mvc.perform(post("/api/push/subscripcions").principal(JwtDeProva.sessio("u-1", "CLIENT")).contentType(MediaType.APPLICATION_JSON).content("{\"endpoint\":\"https://fcm.googleapis.com/x\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Donar de baixa només afecta l'usuari de la sessió")
	void baixa() throws Exception {
		mvc.perform(delete("/api/push/subscripcions").principal(JwtDeProva.sessio("u-1", "CLIENT")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"endpoint\":\"https://fcm.googleapis.com/x\"}")).andExpect(status().isNoContent());
		org.mockito.Mockito.verify(servei).donarDeBaixa("u-1", "https://fcm.googleapis.com/x");
	}
}
