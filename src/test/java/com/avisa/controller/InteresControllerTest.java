package com.avisa.controller;

import com.avisa.dto.CrearInteresRequest;
import com.avisa.model.Interes;
import com.avisa.model.Tasca;
import com.avisa.service.InteresService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InteresControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InteresService interesService;

    @InjectMocks
    private InteresController interesController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interesController).build();
    }

    @Test
    @DisplayName("POST /api/interessos crea un interès amb 201")
    void haDeCrearInteres() throws Exception {
        CrearInteresRequest req = new CrearInteresRequest("PUB-1", "u-1", "Client A", "Vull el servei");
        Interes created = new Interes("INT-1", "PUB-1", "u-1", "Client A", "Vull el servei", null, LocalDateTime.now());
        when(interesService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/interessos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.missatge").value("Vull el servei"));
    }

    @Test
    @DisplayName("GET /api/interessos/professional/{id} retorna llista")
    void haDeLlistarPerProfessional() throws Exception {
        Interes created = new Interes("INT-1", "PUB-1", "u-1", "Client A", "Vull el servei", null, LocalDateTime.now());
        when(interesService.listByProfessional("pro1")).thenReturn(List.of(created));

        mockMvc.perform(get("/api/interessos/professional/pro1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("INT-1"));
    }

    @Test
    @DisplayName("POST /api/interessos/{id}/acceptar accepta l'interès")
    void haDAcceptarInteres() throws Exception {
        Tasca t = new Tasca("T-1", "Servei", "Desc", "Cat", "u-client", "Client A", "u-pro", "Pro 1", null, null, null, "50€", LocalDateTime.now(), null, null, null, null);
        when(interesService.accept("INT-1")).thenReturn(Optional.of(t));

        mockMvc.perform(post("/api/interessos/INT-1/acceptar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("T-1"));
    }
}

