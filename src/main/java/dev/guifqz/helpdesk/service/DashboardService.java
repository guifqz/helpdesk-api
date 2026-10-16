package dev.guifqz.helpdesk.service;

import dev.guifqz.helpdesk.domain.StatusChamado;
import dev.guifqz.helpdesk.repository.ChamadoRepository;
import dev.guifqz.helpdesk.repository.ContagemStatus;
import dev.guifqz.helpdesk.repository.TempoResolucao;
import dev.guifqz.helpdesk.web.dto.ResumoDashboard;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ChamadoRepository chamados;
    private final Clock clock;

    public DashboardService(ChamadoRepository chamados, Clock clock) {
        this.chamados = chamados;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResumoDashboard resumo() {
        Map<StatusChamado, Long> porStatus = new EnumMap<>(StatusChamado.class);
        for (StatusChamado status : StatusChamado.values()) {
            porStatus.put(status, 0L);
        }
        for (ContagemStatus contagem : chamados.contarPorStatus()) {
            porStatus.put(contagem.status(), contagem.total());
        }
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long vencidos = chamados.countByPrazoSlaBeforeAndStatusNotIn(
                Instant.now(clock), StatusChamado.FINALIZADOS);
        return new ResumoDashboard(total, porStatus, vencidos, tempoMedioResolucaoHoras());
    }

    private Double tempoMedioResolucaoHoras() {
        List<TempoResolucao> tempos = chamados.temposDeResolucao();
        if (tempos.isEmpty()) {
            return null;
        }
        double mediaSegundos = tempos.stream()
                .mapToLong(t -> Duration.between(t.criadoEm(), t.resolvidoEm()).toSeconds())
                .average()
                .orElse(0);
        return Math.round(mediaSegundos / 3600.0 * 100.0) / 100.0;
    }
}
