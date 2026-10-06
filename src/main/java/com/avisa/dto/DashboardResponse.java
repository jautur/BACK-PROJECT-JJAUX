package com.avisa.dto;

import com.avisa.model.Oferta;
import com.avisa.model.Tasca;
import java.util.List;

public record DashboardResponse(int openTasks, int activeOffers, int completedProjects, List<Tasca> tasks, List<Oferta> offers) {
}