package com.avisa.controller;

import com.avisa.config.GlobalExceptionHandler;
import com.avisa.imatges.ServeiImatges;
import com.avisa.model.Usuari;
import com.avisa.service.UsuariService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuariControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuariService usuariService;

    @Mock
    private ServeiImatges imatges;

    @InjectMocks
    private UsuariController usuariController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuariController).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("GET /api/users és només per a l'admin")
    void haDeLlistarUsuaris() throws Exception {
        Usuari u = new Usuari("u-1", "Pep", "pep@test.com", "client", "666111222", "Bio");
        when(usuariService.list()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users").principal(JwtDeProva.sessio("u-1", "CLIENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").principal(JwtDeProva.sessio("u-admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("pep@test.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} retorna usuari per ID")
    void haDeRetornarUsuariPerId() throws Exception {
        Usuari u = new Usuari("u-1", "Pep", "pep@test.com", "client", "666111222", "Bio");
        when(usuariService.findById("u-1")).thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/u-1").principal(JwtDeProva.sessio("u-1", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u-1"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} elimina usuari amb 204 si ho fa un admin")
    void haDEliminarUsuari() throws Exception {
        when(usuariService.delete("u-1")).thenReturn(true);

        mockMvc.perform(delete("/api/users/u-1").principal(JwtDeProva.sessio("u-admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} esborra també els fitxers de la foto i el logo")
    void haDEsborrarLesImatgesDeLUsuari() throws Exception {
        Usuari u = new Usuari("u-1", "Pep", "pep@test.com", "PROFESSIONAL", null, null, 3, null, false, false,
                "/api/imatges/0b6f2c1e-1111-4222-8333-944455556666.jpg", "/api/imatges/1b6f2c1e-1111-4222-8333-944455556666.png");
        when(usuariService.findById("u-1")).thenReturn(Optional.of(u));
        when(usuariService.delete("u-1")).thenReturn(true);

        mockMvc.perform(delete("/api/users/u-1").principal(JwtDeProva.sessio("u-admin", "ADMIN")))
                .andExpect(status().isNoContent());

        verify(imatges).esborrarFitxers(u.fotoUrl());
        verify(imatges).esborrarFitxers(u.logoUrl());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} no esborra cap fitxer si l'usuari no s'ha pogut esborrar")
    void noHaDEsborrarImatgesSiNoEsborraLUsuari() throws Exception {
        when(usuariService.delete("u-1")).thenReturn(false);

        mockMvc.perform(delete("/api/users/u-1").principal(JwtDeProva.sessio("u-admin", "ADMIN")))
                .andExpect(status().isBadRequest());

        verify(imatges, never()).esborrarFitxers(any());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} denega si qui ho demana no és admin")
    void noHaDEliminarUsuariNoAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/u-1").principal(JwtDeProva.sessio("u-client", "CLIENT")))
                .andExpect(status().isForbidden());

        verify(usuariService, never()).delete(any());
    }

    @Test
    @DisplayName("PUT /api/users/{id} denega editar el perfil d'un altre usuari")
    void noHaDEditarPerfilAlie() throws Exception {
        mockMvc.perform(put("/api/users/u-altre")
                .principal(JwtDeProva.sessio("u-client", "CLIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nom", "Nou nom"))))
                .andExpect(status().isForbidden());

        verify(usuariService, never()).update(any(), any(), any(), any());
    }
}
