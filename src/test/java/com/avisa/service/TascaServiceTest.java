package com.avisa.service;

import com.avisa.dto.FinalitzarTascaRequest;
import com.avisa.dto.TascaRequest;
import com.avisa.model.Tasca;
import java.time.LocalDateTime;
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
class TascaServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private TascaService tascaService;

    @Test
    @DisplayName("Llistar totes les tasques")
    void haDeLlistarTasques() {
        Tasca t = new Tasca("T-1", "Reparació", "Aixeta", "Fontaneria", "u-1", "Joan", "u-2", "Manel", null, null, null, "50€", LocalDateTime.now(), null, null, null, null);
        when(catalog.tasks()).thenReturn(List.of(t));

        List<Tasca> list = tascaService.list();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).titol()).isEqualTo("Reparació");
    }

    @Test
    @DisplayName("Trobar tasca per ID")
    void haDeTrobarTascaPerId() {
        Tasca t = new Tasca("T-1", "Reparació", "Aixeta", "Fontaneria", "u-1", "Joan", "u-2", "Manel", null, null, null, "50€", LocalDateTime.now(), null, null, null, null);
        when(catalog.findTaskById("T-1")).thenReturn(Optional.of(t));

        Optional<Tasca> result = tascaService.findById("T-1");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("T-1");
    }

    @Test
    @DisplayName("Crear tasca")
    void haDeCrearTasca() {
        TascaRequest req = new TascaRequest("Neteja", "Neteja a fons", "Neteja", "Client Nom", "Pro Nom", "80€", "En curs", "tasca");
        Tasca t = new Tasca("T-2", "Neteja", "Neteja a fons", "Neteja", "u-1", "Client Nom", "u-2", "Pro Nom", null, null, null, "80€", LocalDateTime.now(), null, null, null, null);
        when(catalog.addTask(req)).thenReturn(t);

        Tasca created = tascaService.create(req);
        assertThat(created).isNotNull();
        assertThat(created.titol()).isEqualTo("Neteja");
    }

    @Test
    @DisplayName("Finalitzar tasca amb valoració")
    void haDeFinalitzarTasca() {
        Tasca tFinalitzada = new Tasca("T-1", "Reparació", "Aixeta", "Fontaneria", "u-1", "Joan", "u-2", "Manel", null, null, null, "50€", LocalDateTime.now(), LocalDateTime.now(), 5, "Feina excel·lent!", LocalDateTime.now());
        when(catalog.completeTask("T-1", 5, "Feina excel·lent!")).thenReturn(Optional.of(tFinalitzada));

        Optional<Tasca> result = tascaService.completeWithReview("T-1", 5, "Feina excel·lent!");
        assertThat(result).isPresent();
        assertThat(result.get().valoracio()).isEqualTo(5);
    }
}
