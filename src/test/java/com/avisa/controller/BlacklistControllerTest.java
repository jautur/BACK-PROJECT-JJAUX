package com.avisa.controller;

import com.avisa.dto.AfegirBlacklistRequest;
import com.avisa.model.BlacklistTelefon;
import com.avisa.service.BlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BlacklistControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BlacklistService blacklistService;

    @InjectMocks
    private BlacklistController blacklistController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(blacklistController).build();
    }

    @Test
    @DisplayName("GET /api/blacklist retorna llista de telèfons bloquejats")
    void haDeLlistarBlacklist() throws Exception {
        BlacklistTelefon b = new BlacklistTelefon("BL-1", "666000111", "Spam", "admin", LocalDateTime.now());
        when(blacklistService.list()).thenReturn(List.of(b));

        mockMvc.perform(get("/api/blacklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].telefon").value("666000111"));
    }

    @Test
    @DisplayName("GET /api/blacklist/check?telefon=666000111 verifica estat")
    void haDeComprovarTelefon() throws Exception {
        when(blacklistService.isBlacklisted("666000111")).thenReturn(true);

        mockMvc.perform(get("/api/blacklist/check").param("telefon", "666000111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blacklisted").value(true));
    }

    @Test
    @DisplayName("POST /api/blacklist afegeix telèfon amb 201 Created")
    void haDAfegirTelefon() throws Exception {
        AfegirBlacklistRequest req = new AfegirBlacklistRequest("666555444", "Spam");
        BlacklistTelefon creat = new BlacklistTelefon("BL-2", "666555444", "Spam", "admin", LocalDateTime.now());
        when(blacklistService.add(any(), eq("admin"), eq("AdminUser"))).thenReturn(creat);

        mockMvc.perform(post("/api/blacklist")
                .header("X-User-Rol", "admin")
                .header("X-User-Nom", "AdminUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefon").value("666555444"));
    }

    @Test
    @DisplayName("DELETE /api/blacklist/{id} desbloqueja amb 204 No Content")
    void haDEsborrarTelefon() throws Exception {
        when(blacklistService.remove("666555444", "admin")).thenReturn(true);

        mockMvc.perform(delete("/api/blacklist/666555444")
                .header("X-User-Rol", "admin"))
                .andExpect(status().isNoContent());
    }
}

