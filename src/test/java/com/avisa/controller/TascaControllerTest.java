package com.avisa.controller;

import com.avisa.dto.FinalitzarTascaRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        mockMvc = MockMvcBuilders.standaloneSetup(tascaController).setControllerAdvice(new com.avisa.config.GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("GET /api/tasks retorna llista de tasques")
    void haDeLlistarTasques() throws Exception {
        Tasca t = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", "u-2", "Pro", null, null, null, "100€", LocalDateTime.now(), null, null, null, null);
        when(tascaService.list()).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks").principal(JwtDeProva.sessio("u-1", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titol").value("Pintura"));
        mockMvc.perform(get("/api/tasks").principal(JwtDeProva.sessio("u-9", "CLIENT")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/tasks?size= pagina a la base per a l'admin i en memòria per a la resta, sense mostrar tasques alienes")
    void haDePaginarTasques() throws Exception {
        Tasca t1 = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", "u-2", "Pro", null, null, null, "100€", LocalDateTime.now(), null, null, null, null);
        Tasca t2 = new Tasca("T-2", "Llum", "Desc", "Electricitat", "u-1", "Client", "u-2", "Pro", null, null, null, "50€", LocalDateTime.now(), null, null, null, null);
        Tasca aliena = new Tasca("T-3", "Aliena", "Desc", "Fontaneria", "u-7", "Altre", "u-8", "Pro", null, null, null, "10€", LocalDateTime.now(), null, null, null, null);
        when(tascaService.list()).thenReturn(List.of(t1, t2, aliena));
        when(tascaService.list(1, 1)).thenReturn(List.of(t2));
        when(tascaService.count()).thenReturn(3L);

        mockMvc.perform(get("/api/tasks").param("size", "1").param("page", "1").principal(JwtDeProva.sessio("a-1", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$[0].id").value("T-2"));
        mockMvc.perform(get("/api/tasks").param("size", "1").param("page", "1").principal(JwtDeProva.sessio("u-1", "CLIENT")))
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("T-2"));
        mockMvc.perform(get("/api/tasks").param("size", "5").param("page", "9").principal(JwtDeProva.sessio("u-1", "CLIENT")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/tasks/disponibles?size= retorna una pàgina i el total")
    void haDePaginarDisponibles() throws Exception {
        Tasca t1 = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", null, null, null, null, null, "100€", LocalDateTime.now(), null, null, null, null);
        Tasca t2 = new Tasca("T-2", "Llum", "Desc", "Electricitat", "u-1", "Client", null, null, null, null, null, "50€", LocalDateTime.now(), null, null, null, null);
        when(tascaService.listAvailable()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/tasks/disponibles").param("size", "1"))
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/tasks/disponibles")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} retorna tasca si existeix")
    void haDeRetornarTascaPerId() throws Exception {
        Tasca t = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", "u-2", "Pro", null, null, null, "100€", LocalDateTime.now(), null, null, null, null);
        when(tascaService.findTaskOrIncidenciaById("T-1")).thenReturn(Optional.of(t));

        mockMvc.perform(get("/api/tasks/T-1").principal(JwtDeProva.sessio("u-1", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("T-1"));
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/finalitzar marca com completada")
    void haDeFinalitzarTasca() throws Exception {
        FinalitzarTascaRequest req = new FinalitzarTascaRequest(5, "Molt bona feina");
        Tasca t = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-1", "Client", "u-2", "Pro", null, null, null, "100€", LocalDateTime.now(), LocalDateTime.now(), 5, "Molt bona feina", LocalDateTime.now());
        when(tascaService.completeWithReview(eq("T-1"), eq("u-1"), eq(5), eq("Molt bona feina"))).thenReturn(Optional.of(t));

        mockMvc.perform(post("/api/tasks/T-1/finalitzar").principal(() -> "u-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valoracio").value(5));
    }
}

