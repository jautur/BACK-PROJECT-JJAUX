package com.avisa.controller;

import com.avisa.dto.CrearPressupostRequest;
import com.avisa.model.Pressupost;
import com.avisa.model.Tasca;
import com.avisa.service.PressupostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PressupostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PressupostService pressupostService;

    @InjectMocks
    private PressupostController pressupostController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pressupostController).build();
    }

    @Test
    @DisplayName("POST /api/pressupostos crea pressupost si el professional és vàlid")
    void haDeCrearPressupost() throws Exception {
        CrearPressupostRequest req = new CrearPressupostRequest("I-1", "u-pro", "Pro Manel", "90€", "Pintura", "1 dia");
        Pressupost p = new Pressupost("P-1", "I-1", "u-pro", "Pro Manel", "90€", "Pintura", "1 dia", LocalDateTime.now());

        when(pressupostService.isProfessionalEligible("u-pro")).thenReturn(true);
        when(pressupostService.create(any())).thenReturn(p);

        mockMvc.perform(post("/api/pressupostos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preu").value("90€"));
    }

    @Test
    @DisplayName("POST /api/pressupostos retorna 403 si un client intenta pressupostar")
    void noHaDePermetreClientPressupostar() throws Exception {
        CrearPressupostRequest req = new CrearPressupostRequest("I-1", "u-client", "Client Joan", "90€", "Pintura", "1 dia");

        when(pressupostService.isProfessionalEligible("u-client")).thenReturn(false);

        mockMvc.perform(post("/api/pressupostos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/pressupostos/{id} retorna 403 si l'usuari no està implicat")
    void haDeRetornar403SiNoEstaImplicat() throws Exception {
        Pressupost p = new Pressupost("P-1", "I-1", "u-pro", "Pro Manel", "90€", "Pintura", "1 dia", LocalDateTime.now());
        when(pressupostService.findById("P-1")).thenReturn(Optional.of(p));
        when(pressupostService.isUserInvolved(eq(p), eq("u-desconegut"), any(), any())).thenReturn(false);

        mockMvc.perform(get("/api/pressupostos/P-1")
                .header("X-User-Id", "u-desconegut"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/pressupostos/{id} retorna 200 si l'usuari està implicat")
    void haDeRetornar200SiEstaImplicat() throws Exception {
        Pressupost p = new Pressupost("P-1", "I-1", "u-pro", "Pro Manel", "90€", "Pintura", "1 dia", LocalDateTime.now());
        when(pressupostService.findById("P-1")).thenReturn(Optional.of(p));
        when(pressupostService.isUserInvolved(eq(p), eq("u-pro"), any(), any())).thenReturn(true);

        mockMvc.perform(get("/api/pressupostos/P-1")
                .header("X-User-Id", "u-pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("P-1"));
    }

    @Test
    @DisplayName("POST /api/pressupostos/{id}/acceptar accepta el pressupost i genera la tasca")
    void haDAcceptarPressupost() throws Exception {
        Tasca t = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-client", "Client", "u-pro", "Pro", null, null, null, "90€", LocalDateTime.now(), null, null, null, null);
        when(pressupostService.accept("P-1")).thenReturn(Optional.of(t));

        mockMvc.perform(post("/api/pressupostos/P-1/acceptar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("T-1"));
    }

    @Test
    @DisplayName("POST /api/pressupostos/{id}/rebutjar rebutja el pressupost amb 204")
    void haDeRebutjarPressupost() throws Exception {
        when(pressupostService.reject("P-1")).thenReturn(true);

        mockMvc.perform(post("/api/pressupostos/P-1/rebutjar"))
                .andExpect(status().isNoContent());
    }
}

