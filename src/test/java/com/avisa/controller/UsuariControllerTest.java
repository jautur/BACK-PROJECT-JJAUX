package com.avisa.controller;

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

import static org.mockito.Mockito.when;
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

    @InjectMocks
    private UsuariController usuariController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuariController).build();
    }

    @Test
    @DisplayName("GET /api/users retorna llista d'usuaris")
    void haDeLlistarUsuaris() throws Exception {
        Usuari u = new Usuari("u-1", "Pep", "pep@test.com", "client", "666111222", "Bio");
        when(usuariService.list()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Pep"));
    }

    @Test
    @DisplayName("GET /api/users/{id} retorna usuari per ID")
    void haDeRetornarUsuariPerId() throws Exception {
        Usuari u = new Usuari("u-1", "Pep", "pep@test.com", "client", "666111222", "Bio");
        when(usuariService.findById("u-1")).thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u-1"));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/foto assigna foto per admin")
    void haDAssignarFotoAdmin() throws Exception {
        Usuari uAmbFoto = new Usuari("u-pro", "Pro", "pro@test.com", "professional", null, null, 3, null, true, false, "https://foto.jpg");
        when(usuariService.assignarFoto("u-pro", "https://foto.jpg")).thenReturn(uAmbFoto);

        mockMvc.perform(put("/api/users/u-pro/foto")
                .header("X-User-Rol", "admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("foto", "https://foto.jpg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foto").value("https://foto.jpg"));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/foto denega si no és admin")
    void noHaDAssignarFotoNoAdmin() throws Exception {
        mockMvc.perform(put("/api/users/u-pro/foto")
                .header("X-User-Rol", "client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("foto", "https://foto.jpg"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} elimina usuari amb 204")
    void haDEliminarUsuari() throws Exception {
        when(usuariService.delete("u-1")).thenReturn(true);

        mockMvc.perform(delete("/api/users/u-1"))
                .andExpect(status().isNoContent());
    }
}

