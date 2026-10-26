package com.avisa.controller;

import com.avisa.model.Avis;
import com.avisa.service.AvisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AvisControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AvisService avisService;

    @InjectMocks
    private AvisController avisController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(avisController).build();
    }

    @Test
    @DisplayName("GET /api/avisos retorna llista d'avisos")
    void haDeLlistarAvisos() throws Exception {
        Avis a = new Avis("A-1", "u-1", "Notificació de prova", false, LocalDate.now());
        when(avisService.list()).thenReturn(List.of(a));

        mockMvc.perform(get("/api/avisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].missatge").value("Notificació de prova"));
    }

    @Test
    @DisplayName("GET /api/avisos/usuari/{id} retorna avisos d'usuari")
    void haDeLlistarPerUsuari() throws Exception {
        Avis a = new Avis("A-1", "u-1", "Notificació de prova", false, LocalDate.now());
        when(avisService.listByUsuari("u-1")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/avisos/usuari/u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuariId").value("u-1"));
    }

    @Test
    @DisplayName("POST /api/avisos crea un avís")
    void haDeCrearAvis() throws Exception {
        Avis nou = new Avis("A-2", "u-2", "Nova tasca", false, LocalDate.now());
        when(avisService.create("u-2", "Nova tasca")).thenReturn(nou);

        mockMvc.perform(post("/api/avisos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuariId\":\"u-2\",\"missatge\":\"Nova tasca\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("A-2"));
    }
}

