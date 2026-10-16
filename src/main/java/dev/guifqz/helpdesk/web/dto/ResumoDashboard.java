package dev.guifqz.helpdesk.web.dto;

import dev.guifqz.helpdesk.domain.StatusChamado;
import java.util.Map;

public record ResumoDashboard(
        long totalChamados,
        Map<StatusChamado, Long> porStatus,
        long slaVencido,
        Double tempoMedioResolucaoHoras) {
}
