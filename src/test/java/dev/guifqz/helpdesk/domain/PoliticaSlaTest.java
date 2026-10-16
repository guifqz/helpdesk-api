package dev.guifqz.helpdesk.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class PoliticaSlaTest {

    private static final Instant INICIO = Instant.parse("2026-01-10T12:00:00Z");

    @ParameterizedTest
    @CsvSource({
            "CRITICA, 4",
            "ALTA, 8",
            "MEDIA, 24",
            "BAIXA, 72"
    })
    void calculaPrazoPorPrioridade(Prioridade prioridade, long horas) {
        assertThat(PoliticaSla.calcularPrazo(INICIO, prioridade)).isEqualTo(INICIO.plus(Duration.ofHours(horas)));
    }

    @Test
    void chamadoEmAbertoAposOPrazoEstaVencido() {
        Instant prazo = PoliticaSla.calcularPrazo(INICIO, Prioridade.CRITICA);

        assertThat(PoliticaSla.vencido(prazo, StatusChamado.ABERTO, prazo.plusSeconds(1))).isTrue();
    }

    @Test
    void chamadoDentroDoPrazoNaoEstaVencido() {
        Instant prazo = PoliticaSla.calcularPrazo(INICIO, Prioridade.ALTA);

        assertThat(PoliticaSla.vencido(prazo, StatusChamado.EM_ATENDIMENTO, prazo.minusSeconds(1))).isFalse();
        assertThat(PoliticaSla.vencido(prazo, StatusChamado.EM_ATENDIMENTO, prazo)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"RESOLVIDO", "FECHADO"})
    void chamadoFinalizadoNuncaEstaVencido(StatusChamado status) {
        Instant prazo = PoliticaSla.calcularPrazo(INICIO, Prioridade.CRITICA);

        assertThat(PoliticaSla.vencido(prazo, status, prazo.plus(Duration.ofDays(30)))).isFalse();
    }
}
