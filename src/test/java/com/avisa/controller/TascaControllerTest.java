package com.avisa.controller;

import com.avisa.dto.FinalitzarTascaRequest;
import com.avisa.dto.TascaRequest;
import com.avisa.model.Tasca;
import com.avisa.service.TascaService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TascaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TascaService tascaService;

    @InjectMocks
    private TascaController tascaController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tascaController).build();
    }

    @Test
    @DisplayName("GET /api/tasks retorna llista de tasques")
    void haDeLlistarTasques() throws Exception {
        Tasca t = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", "u-2", "Pro", null, null, null, "100€", LocalDateTime.now(), null, null, null, null);
        when(tascaService.list()).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titol").value("Pintura"));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} retorna tasca si existeix")
    void haDeRetornarTascaPerId() throws Exception {
        Tasca t = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", "u-2", "Pro", null, null, null, "100€", LocalDateTime.now(), null, null, null, null);
        when(tascaService.findTaskOrIncidenciaById("T-1")).thenReturn(Optional.of(t));

        mockMvc.perform(get("/api/tasks/T-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("T-1"));
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/finalitzar marca com completada")
    void haDeFinalitzarTasca() throws Exception {
        FinalitzarTascaRequest req = new FinalitzarTascaRequest(5, "Molt bona feina");
        Tasca t = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", "u-2", "Pro", null, null, null, "100€", LocalDateTime.now(), LocalDateTime.now(), 5, "Molt bona feina", LocalDateTime.now());
        when(tascaService.completeWithReview(eq("T-1"), eq(5), eq("Molt bona feina"))).thenReturn(Optional.of(t));

        mockMvc.perform(post("/api/tasks/T-1/finalitzar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracio").value(5));
    }
}

