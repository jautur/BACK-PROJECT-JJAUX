package com.avisa.controller;

import com.avisa.dto.OfertaRequest;
import com.avisa.model.Oferta;
import com.avisa.service.OfertaService;
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
class OfertaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OfertaService ofertaService;

    @InjectMocks
    private OfertaController ofertaController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ofertaController).build();
    }

    @Test
    @DisplayName("GET /api/offers retorna llista d'ofertes")
    void haDeLlistarOfertes() throws Exception {
        Oferta o = new Oferta("O-1", "standalone", "Pro Manel", "Fontaneria", "45€/h", "Reparacions", "Activa");
        when(ofertaService.list()).thenReturn(List.of(o));

        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].professional").value("Pro Manel"));
    }

    @Test
    @DisplayName("GET /api/offers/{id} retorna oferta")
    void haDeRetornarOfertaPerId() throws Exception {
        Oferta o = new Oferta("O-1", "standalone", "Pro Manel", "Fontaneria", "45€/h", "Reparacions", "Activa");
        when(ofertaService.findById("O-1")).thenReturn(Optional.of(o));

        mockMvc.perform(get("/api/offers/O-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O-1"));
    }

    @Test
    @DisplayName("POST /api/offers crea oferta")
    void haDeCrearOferta() throws Exception {
        OfertaRequest req = new OfertaRequest("standalone", "Pro Manel", "Fontaneria", "45€/h", "Reparacions", "Activa");
        Oferta o = new Oferta("O-2", "standalone", "Pro Manel", "Fontaneria", "45€/h", "Reparacions", "Activa");
        when(ofertaService.create(any())).thenReturn(o);

        mockMvc.perform(post("/api/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O-2"));
    }
}

