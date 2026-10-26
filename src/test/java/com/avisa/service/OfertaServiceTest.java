package com.avisa.service;

import com.avisa.dto.OfertaRequest;
import com.avisa.model.Oferta;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfertaServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private OfertaService ofertaService;

    @Test
    @DisplayName("Llistar totes les ofertes")
    void haDeLlistarOfertes() {
        Oferta o = new Oferta("O-1", "standalone", "Pro Jordi", "Jardineria", "30€/h", "Tall de gespa", "Activa");
        when(catalog.offers()).thenReturn(List.of(o));

        List<Oferta> result = ofertaService.list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoria()).isEqualTo("Jardineria");
    }

    @Test
    @DisplayName("Trobar oferta per ID")
    void haDeTrobarOfertaPerId() {
        Oferta o = new Oferta("O-1", "standalone", "Pro Jordi", "Jardineria", "30€/h", "Tall de gespa", "Activa");
        when(catalog.findOfferById("O-1")).thenReturn(Optional.of(o));

        Optional<Oferta> result = ofertaService.findById("O-1");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("O-1");
    }

    @Test
    @DisplayName("Crear oferta nova")
    void haDeCrearOferta() {
        OfertaRequest req = new OfertaRequest("standalone", "Pro Jordi", "Jardineria", "30€/h", "Tall de gespa", "Activa");
        Oferta o = new Oferta("O-2", "standalone", "Pro Jordi", "Jardineria", "30€/h", "Tall de gespa", "Activa");
        when(catalog.addOffer(req)).thenReturn(o);

        Oferta created = ofertaService.create(req);
        assertThat(created).isNotNull();
        assertThat(created.professional()).isEqualTo("Pro Jordi");
    }
}

