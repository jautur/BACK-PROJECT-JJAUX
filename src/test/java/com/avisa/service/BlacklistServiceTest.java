package com.avisa.service;

import com.avisa.dto.AfegirBlacklistRequest;
import com.avisa.model.BlacklistTelefon;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private BlacklistService blacklistService;

    @Test
    @DisplayName("Llistar números a la llista negra")
    void haDeLlistarBlacklist() {
        BlacklistTelefon b = new BlacklistTelefon("BL-1", "666000111", "Spam", "admin", LocalDateTime.now());
        when(catalog.blacklistTelefons()).thenReturn(List.of(b));

        List<BlacklistTelefon> result = blacklistService.list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).telefon()).isEqualTo("666000111");
    }

    @Test
    @DisplayName("Comprovar si un telèfon està bloquejat")
    void haDeComprovarSiEstaBloquejat() {
        when(catalog.isPhoneBlacklisted("666000111")).thenReturn(true);
        when(catalog.isPhoneBlacklisted("600000000")).thenReturn(false);

        assertThat(blacklistService.isBlacklisted("666000111")).isTrue();
        assertThat(blacklistService.isBlacklisted("600000000")).isFalse();
    }

    @Test
    @DisplayName("Afegir un telèfon a la llista negra")
    void haDAfegirTelefon() {
        AfegirBlacklistRequest req = new AfegirBlacklistRequest("666222333", "Frau reiterat");
        BlacklistTelefon b = new BlacklistTelefon("BL-2", "666222333", "Frau reiterat", "admin", LocalDateTime.now());
        when(catalog.addToBlacklist("666222333", "Frau reiterat", "admin", "AdminUser")).thenReturn(b);

        BlacklistTelefon creat = blacklistService.add(req, "admin", "AdminUser");
        assertThat(creat).isNotNull();
        assertThat(creat.telefon()).isEqualTo("666222333");
    }

    @Test
    @DisplayName("Eliminar un telèfon de la llista negra")
    void haDEliminarTelefon() {
        when(catalog.removeFromBlacklist("666222333", "admin")).thenReturn(true);

        boolean esborrat = blacklistService.remove("666222333", "admin");
        assertThat(esborrat).isTrue();
        verify(catalog).removeFromBlacklist("666222333", "admin");
    }
}

