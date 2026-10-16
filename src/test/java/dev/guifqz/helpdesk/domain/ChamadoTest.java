package dev.guifqz.helpdesk.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.guifqz.helpdesk.exception.RegraNegocioException;
import dev.guifqz.helpdesk.exception.TransicaoInvalidaException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChamadoTest {

    private static final Instant CRIACAO = Instant.parse("2026-03-02T09:00:00Z");

    private Usuario solicitante;
    private Chamado chamado;

    @BeforeEach
    void preparar() {
        solicitante = new Usuario("Maria", "maria@empresa.com", "hash", Papel.SOLICITANTE);
        chamado = new Chamado("VPN fora do ar", "Sem acesso desde cedo", Categoria.REDE,
                Prioridade.ALTA, solicitante, CRIACAO);
    }

    @Test
    void nasceAbertoComPrazoDeSlaDaPrioridade() {
        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getPrazoSla()).isEqualTo(CRIACAO.plus(Duration.ofHours(8)));
        assertThat(chamado.getResolvidoEm()).isNull();
        assertThat(chamado.getTecnico()).isNull();
    }

    @Test
    void resolverRegistraDataDeResolucao() {
        Instant atendimento = CRIACAO.plusSeconds(600);
        Instant resolucao = CRIACAO.plusSeconds(3600);

        chamado.alterarStatus(StatusChamado.EM_ATENDIMENTO, atendimento);
        chamado.alterarStatus(StatusChamado.RESOLVIDO, resolucao);

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.RESOLVIDO);
        assertThat(chamado.getResolvidoEm()).isEqualTo(resolucao);
        assertThat(chamado.getAtualizadoEm()).isEqualTo(resolucao);
    }

    @Test
    void reabrirLimpaDataDeResolucao() {
        chamado.alterarStatus(StatusChamado.EM_ATENDIMENTO, CRIACAO.plusSeconds(60));
        chamado.alterarStatus(StatusChamado.RESOLVIDO, CRIACAO.plusSeconds(120));

        chamado.alterarStatus(StatusChamado.EM_ATENDIMENTO, CRIACAO.plusSeconds(180));

        assertThat(chamado.getResolvidoEm()).isNull();
    }

    @Test
    void transicaoInvalidaMantemEstadoOriginal() {
        assertThatThrownBy(() -> chamado.alterarStatus(StatusChamado.RESOLVIDO, CRIACAO.plusSeconds(60)))
                .isInstanceOf(TransicaoInvalidaException.class);

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getAtualizadoEm()).isEqualTo(CRIACAO);
    }

    @Test
    void mudarPrioridadeRecalculaSlaAPartirDaCriacao() {
        chamado.atualizarDados("VPN fora do ar", "Sem acesso", Categoria.REDE, Prioridade.CRITICA,
                CRIACAO.plusSeconds(1800));

        assertThat(chamado.getPrazoSla()).isEqualTo(CRIACAO.plus(Duration.ofHours(4)));
    }

    @Test
    void chamadoFechadoNaoPodeSerAtribuidoNemAlterado() {
        Usuario tecnico = new Usuario("Joao", "joao@empresa.com", "hash", Papel.TECNICO);
        chamado.alterarStatus(StatusChamado.FECHADO, CRIACAO.plusSeconds(60));

        assertThatThrownBy(() -> chamado.atribuir(tecnico, CRIACAO.plusSeconds(120)))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> chamado.atualizarDados("x", "y", Categoria.ERP, Prioridade.BAIXA,
                CRIACAO.plusSeconds(120)))
                .isInstanceOf(RegraNegocioException.class);
    }
}
