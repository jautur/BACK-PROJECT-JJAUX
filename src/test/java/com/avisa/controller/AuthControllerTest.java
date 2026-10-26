package com.avisa.controller;

import com.avisa.dto.DemanaRecuperacioRequest;
import com.avisa.dto.LoginRequest;
import com.avisa.dto.RecuperacioResponse;
import com.avisa.dto.RegisterRequest;
import com.avisa.model.Usuari;
import com.avisa.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("GET /api/auth/me retorna 200 quan hi ha usuari")
    void haDeRetornarUsuariMe() throws Exception {
        Usuari u = new Usuari("u-1", "Joan", "joan@test.com", "client", null, null);
        when(authService.getActiveUser()).thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Joan"));
    }

    @Test
    @DisplayName("POST /api/auth/login retorna 200 amb usuari autenticat")
    void haDeFerLoginOk() throws Exception {
        LoginRequest req = new LoginRequest("joan@test.com", "secret123");
        Usuari u = new Usuari("u-1", "Joan", "joan@test.com", "client", null, null).withToken("token-test");
        when(authService.login(any())).thenReturn(Optional.of(u));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-test"));
    }

    @Test
    @DisplayName("POST /api/auth/login retorna 401 si les credencials són incorrectes")
    void haDeRetornar401LoginIncorrecte() throws Exception {
        LoginRequest req = new LoginRequest("erroni@test.com", "wrong");
        when(authService.login(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/demanar-recuperacio retorna 200 amb codi de verificació")
    void haDeRetornarRecuperacioOk() throws Exception {
        DemanaRecuperacioRequest req = new DemanaRecuperacioRequest("joan@test.com");
        RecuperacioResponse resp = new RecuperacioResponse(true, "Codi generat", "joan@test.com", "123456");
        when(authService.sollicitarRecuperacio(any())).thenReturn(resp);

        mockMvc.perform(post("/api/auth/demanar-recuperacio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.codiDev").value("123456"));
    }
}

