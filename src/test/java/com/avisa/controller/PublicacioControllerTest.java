package com.avisa.controller;

import com.avisa.model.Publicacio;
import com.avisa.model.Tasca;
import com.avisa.service.PublicacioService;
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
class PublicacioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PublicacioService publicacioService;

    @InjectMocks
    private PublicacioController publicacioController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicacioController).build();
    }

    @Test
    @DisplayName("GET /api/publicacions retorna llista de publicacions")
    void haDeLlistarPublicacions() throws Exception {
        Publicacio p = new Publicacio("PUB-1", "u-1", "Joan", "OFERTA", "Pintura", "Desc", "Pintura", "50€", LocalDateTime.now());
        when(publicacioService.list()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/publicacions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titol").value("Pintura"));
    }

    @Test
    @DisplayName("GET /api/publicacions?tipus=INCIDENCIA filtra per tipus")
    void haDeFiltrarPerTipus() throws Exception {
        Publicacio p = new Publicacio("I-1", "u-1", "Joan", "INCIDENCIA", "Avaria", "Desc", "Fontaneria", "50€", LocalDateTime.now());
        when(publicacioService.listByTipus("INCIDENCIA")).thenReturn(List.of(p));

        mockMvc.perform(get("/api/publicacions").param("tipus", "INCIDENCIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipus").value("INCIDENCIA"));
    }

    @Test
    @DisplayName("POST /api/publicacions crea incidència amb 201")
    void haDeCrearIncidencia() throws Exception {
        Publicacio req = new Publicacio(null, "u-client", "Maria", "INCIDENCIA", "Persiana trencada", "Desc", "Persianes", "40€", null);
        Publicacio created = new Publicacio("I-12345678", "u-client", "Maria", "INCIDENCIA", "Persiana trencada", "Desc", "Persianes", "40€", LocalDateTime.now());
        when(publicacioService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/publicacions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titol").value("Persiana trencada"));
    }
}

