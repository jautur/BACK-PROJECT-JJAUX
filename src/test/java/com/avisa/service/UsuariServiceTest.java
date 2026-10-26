package com.avisa.service;

import com.avisa.model.Usuari;
import java.util.List;
import java.util.Optional;
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
class UsuariServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private UsuariService usuariService;

    @Test
    @DisplayName("Llistar tots els usuaris")
    void haDeLlistarUsuaris() {
        Usuari u = new Usuari("u-1", "Pep", "pep@test.com", "client", "666111222", "Bio");
        when(catalog.users()).thenReturn(List.of(u));

        List<Usuari> result = usuariService.list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nom()).isEqualTo("Pep");
    }

    @Test
    @DisplayName("Trobar usuari per ID")
    void haDeTrobarPerId() {
        Usuari u = new Usuari("u-1", "Pep", "pep@test.com", "client", "666111222", "Bio");
        when(catalog.findUserById("u-1")).thenReturn(Optional.of(u));

        Optional<Usuari> result = usuariService.findById("u-1");
        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("pep@test.com");
    }

    @Test
    @DisplayName("Assignar fotografia a professional")
    void haDAssignarFoto() {
        Usuari uAmbFoto = new Usuari("u-1", "Pep", "pep@test.com", "professional", "666111222", "Bio", 3, null, true, false, "https://foto.jpg");
        when(catalog.assignarFoto("u-1", "https://foto.jpg")).thenReturn(uAmbFoto);

        Usuari res = usuariService.assignarFoto("u-1", "https://foto.jpg");
        assertThat(res.foto()).isEqualTo("https://foto.jpg");
    }

    @Test
    @DisplayName("Eliminar usuari")
    void haDEliminarUsuari() {
        when(catalog.deleteUser("u-1")).thenReturn(true);

        boolean esborrat = usuariService.delete("u-1");
        assertThat(esborrat).isTrue();
        verify(catalog).deleteUser("u-1");
    }
}

