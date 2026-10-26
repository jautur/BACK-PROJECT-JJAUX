package com.avisa.controller;

import com.avisa.model.Resenya;
import com.avisa.service.ResenyaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
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
class ResenyaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ResenyaService resenyaService;

    @InjectMocks
    private ResenyaController resenyaController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resenyaController).build();
    }

    @Test
    @DisplayName("GET /api/resenyes retorna llista de ressenyes")
    void haDeLlistarResenyes() throws Exception {
        Resenya r = new Resenya("R-1", "u-client", "u-pro", "T-1", 5, "Molt recomanable", LocalDateTime.now());
        when(resenyaService.list()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/resenyes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].puntuacio").value(5));
    }

    @Test
    @DisplayName("POST /api/resenyes desa una nova valoració")
    void haDeCrearResenya() throws Exception {
        Resenya r = new Resenya("R-2", "u-client", "u-pro", "T-2", 4, "Bona feina", LocalDateTime.now());
        when(resenyaService.create(any())).thenReturn(r);

        mockMvc.perform(post("/api/resenyes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientId\":\"u-client\",\"treballadorId\":\"u-pro\",\"tascaId\":\"T-2\",\"puntuacio\":4,\"descripcio\":\"Bona feina\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntuacio").value(4));
    }
}
