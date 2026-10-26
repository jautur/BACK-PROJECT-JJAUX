package com.avisa.controller;

import com.avisa.dto.IncidenciaRequest;
import com.avisa.model.Incidencia;
import com.avisa.service.IncidenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class IncidenciaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IncidenciaService incidenciaService;

    @InjectMocks
    private IncidenciaController incidenciaController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(incidenciaController).build();
    }

    @Test
    @DisplayName("GET /api/incidencies retorna llista")
    void haDeLlistarIncidencies() throws Exception {
        Incidencia inc = new Incidencia("I-1", "Client A", "Avaria llum", "Curtecircuit", "Electricitat", "60€", "Oberta");
        when(incidenciaService.list()).thenReturn(List.of(inc));

        mockMvc.perform(get("/api/incidencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titol").value("Avaria llum"));
    }

    @Test
    @DisplayName("GET /api/incidencies/{id} retorna 200")
    void haDeRetornarIncidenciaPerId() throws Exception {
        Incidencia inc = new Incidencia("I-1", "Client A", "Avaria llum", "Curtecircuit", "Electricitat", "60€", "Oberta");
        when(incidenciaService.findById("I-1")).thenReturn(Optional.of(inc));

        mockMvc.perform(get("/api/incidencies/I-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("I-1"));
    }

    @Test
    @DisplayName("POST /api/incidencies crea incidència")
    void haDeCrearIncidencia() throws Exception {
        IncidenciaRequest req = new IncidenciaRequest("u-client", "Client A", "Client A", "Avaria persianes", "Cinta trencada", "Persianes", "40€", "Oberta");
        Incidencia inc = new Incidencia("I-2", "Client A", "Avaria persianes", "Cinta trencada", "Persianes", "40€", "Oberta");
        when(incidenciaService.create(any())).thenReturn(inc);

        mockMvc.perform(post("/api/incidencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titol").value("Avaria persianes"));
    }
}

