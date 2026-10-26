package com.avisa.service;

import com.avisa.model.Resenya;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenyaServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private ResenyaService resenyaService;

    @Test
    @DisplayName("Llistar totes les ressenyes")
    void haDeLlistarResenyes() {
        Resenya r = new Resenya("R-1", "u-client", "u-pro", "T-1", 5, "Molt bo", LocalDateTime.now());
        when(catalog.resenyes()).thenReturn(List.of(r));

        List<Resenya> list = resenyaService.list();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).puntuacio()).isEqualTo(5);
    }

    @Test
    @DisplayName("Llistar ressenyes per treballador")
    void haDeLlistarPerTreballador() {
        Resenya r = new Resenya("R-1", "u-client", "u-pro", "T-1", 5, "Molt bo", LocalDateTime.now());
        when(catalog.findResenyesByTreballador("u-pro")).thenReturn(List.of(r));

        List<Resenya> list = resenyaService.listByTreballador("u-pro");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).treballadorId()).isEqualTo("u-pro");
    }

    @Test
    @DisplayName("Crear nova ressenya")
    void haDeCrearResenya() {
        Resenya r = new Resenya("R-2", "u-client", "u-pro", "T-2", 4, "Bona atenció", LocalDateTime.now());
        when(catalog.addResenya(r)).thenReturn(r);

        Resenya creat = resenyaService.create(r);
        assertThat(creat).isNotNull();
        assertThat(creat.descripcio()).isEqualTo("Bona atenció");
    }
}

