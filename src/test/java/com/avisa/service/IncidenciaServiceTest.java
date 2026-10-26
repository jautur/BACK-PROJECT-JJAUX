package com.avisa.service;

import com.avisa.dto.IncidenciaRequest;
import com.avisa.model.Incidencia;
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
class IncidenciaServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private IncidenciaService incidenciaService;

    @Test
    @DisplayName("Llistar totes les incidències")
    void haDeLlistarIncidencies() {
        Incidencia inc = new Incidencia("I-1", "Client A", "Avaria llum", "Curtecircuit", "Electricitat", "60€", "Oberta");
        when(catalog.incidencies()).thenReturn(List.of(inc));

        List<Incidencia> list = incidenciaService.list();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).titol()).isEqualTo("Avaria llum");
    }

    @Test
    @DisplayName("Trobar incidència per ID")
    void haDeTrobarPerId() {
        Incidencia inc = new Incidencia("I-1", "Client A", "Avaria llum", "Curtecircuit", "Electricitat", "60€", "Oberta");
        when(catalog.findIncidenciaById("I-1")).thenReturn(Optional.of(inc));

        Optional<Incidencia> result = incidenciaService.findById("I-1");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("I-1");
    }

    @Test
    @DisplayName("Llistar incidències per client")
    void haDeLlistarPerClient() {
        Incidencia inc = new Incidencia("I-1", "Client A", "Avaria llum", "Curtecircuit", "Electricitat", "60€", "Oberta");
        when(catalog.findIncidenciesByClient("Client A")).thenReturn(List.of(inc));

        List<Incidencia> result = incidenciaService.listByClient("Client A");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).clientNom()).isEqualTo("Client A");
    }

    @Test
    @DisplayName("Crear incidència nova")
    void haDeCrearIncidencia() {
        IncidenciaRequest req = new IncidenciaRequest("u-client", "Client A", "Client A", "Avaria persiana", "S'ha trencat la cinta", "Persianes", "40€", "Oberta");
        Incidencia inc = new Incidencia("I-2", "Client A", "Avaria persiana", "S'ha trencat la cinta", "Persianes", "40€", "Oberta");
        when(catalog.addIncidencia(req)).thenReturn(inc);

        Incidencia created = incidenciaService.create(req);
        assertThat(created).isNotNull();
        assertThat(created.titol()).isEqualTo("Avaria persiana");
    }
}
