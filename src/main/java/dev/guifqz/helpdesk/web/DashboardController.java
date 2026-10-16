package dev.guifqz.helpdesk.web;

import dev.guifqz.helpdesk.service.DashboardService;
import dev.guifqz.helpdesk.web.dto.ResumoDashboard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Contagem por status, chamados com SLA vencido e tempo médio de resolução (TECNICO/ADMIN)")
    public ResumoDashboard resumo() {
        return dashboardService.resumo();
    }
}
