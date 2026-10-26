package com.avisa.service;

import com.avisa.dto.DashboardResponse;
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
class DashboardServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Obtenir dashboard consolidat")
    void haDObtenirDashboard() {
        DashboardResponse mockDashboard = new DashboardResponse(5, 10, 20, List.of(), List.of(), List.of());
        when(catalog.dashboard()).thenReturn(mockDashboard);

        DashboardResponse resp = dashboardService.getDashboard();
        assertThat(resp).isNotNull();
        assertThat(resp.openTasks()).isEqualTo(5);
        assertThat(resp.activeOffers()).isEqualTo(10);
        assertThat(resp.completedProjects()).isEqualTo(20);
    }
}

