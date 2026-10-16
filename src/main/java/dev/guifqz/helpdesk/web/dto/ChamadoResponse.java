package dev.guifqz.helpdesk.web.dto;

import dev.guifqz.helpdesk.domain.Categoria;
import dev.guifqz.helpdesk.domain.Chamado;
import dev.guifqz.helpdesk.domain.PoliticaSla;
import dev.guifqz.helpdesk.domain.Prioridade;
import dev.guifqz.helpdesk.domain.StatusChamado;
import java.time.Instant;

public record ChamadoResponse(
        Long id,
        String titulo,
        String descricao,
        Categoria categoria,
        Prioridade prioridade,
        StatusChamado status,
        UsuarioResumo solicitante,
        UsuarioResumo tecnico,
        Instant criadoEm,
        Instant atualizadoEm,
        Instant resolvidoEm,
        Instant prazoSla,
        boolean slaVencido) {

    public static ChamadoResponse de(Chamado chamado, Instant agora) {
        return new ChamadoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getCategoria(),
                chamado.getPrioridade(),
                chamado.getStatus(),
                UsuarioResumo.de(chamado.getSolicitante()),
                UsuarioResumo.de(chamado.getTecnico()),
                chamado.getCriadoEm(),
                chamado.getAtualizadoEm(),
                chamado.getResolvidoEm(),
                chamado.getPrazoSla(),
                PoliticaSla.vencido(chamado.getPrazoSla(), chamado.getStatus(), agora));
    }
}
