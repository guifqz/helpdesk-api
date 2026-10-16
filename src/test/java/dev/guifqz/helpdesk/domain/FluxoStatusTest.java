package dev.guifqz.helpdesk.domain;

import static dev.guifqz.helpdesk.domain.StatusChamado.ABERTO;
import static dev.guifqz.helpdesk.domain.StatusChamado.AGUARDANDO_USUARIO;
import static dev.guifqz.helpdesk.domain.StatusChamado.EM_ATENDIMENTO;
import static dev.guifqz.helpdesk.domain.StatusChamado.FECHADO;
import static dev.guifqz.helpdesk.domain.StatusChamado.RESOLVIDO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.guifqz.helpdesk.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class FluxoStatusTest {

    @ParameterizedTest
    @CsvSource({
            "ABERTO, EM_ATENDIMENTO",
            "ABERTO, FECHADO",
            "EM_ATENDIMENTO, AGUARDANDO_USUARIO",
            "EM_ATENDIMENTO, RESOLVIDO",
            "AGUARDANDO_USUARIO, EM_ATENDIMENTO",
            "AGUARDANDO_USUARIO, FECHADO",
            "RESOLVIDO, EM_ATENDIMENTO",
            "RESOLVIDO, FECHADO"
    })
    void permiteTransicoesValidas(StatusChamado origem, StatusChamado destino) {
        assertThat(FluxoStatus.permitida(origem, destino)).isTrue();
        assertThatCode(() -> FluxoStatus.validar(origem, destino)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "ABERTO, RESOLVIDO",
            "ABERTO, AGUARDANDO_USUARIO",
            "EM_ATENDIMENTO, ABERTO",
            "EM_ATENDIMENTO, FECHADO",
            "AGUARDANDO_USUARIO, RESOLVIDO",
            "RESOLVIDO, ABERTO",
            "RESOLVIDO, AGUARDANDO_USUARIO"
    })
    void rejeitaTransicoesInvalidas(StatusChamado origem, StatusChamado destino) {
        assertThat(FluxoStatus.permitida(origem, destino)).isFalse();
        assertThatThrownBy(() -> FluxoStatus.validar(origem, destino))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessageContaining(origem.name())
                .hasMessageContaining(destino.name());
    }

    @ParameterizedTest
    @EnumSource(StatusChamado.class)
    void nenhumStatusTransitaParaSiMesmo(StatusChamado status) {
        assertThat(FluxoStatus.permitida(status, status)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(StatusChamado.class)
    void fechadoEhEstadoTerminal(StatusChamado destino) {
        assertThat(FluxoStatus.permitida(FECHADO, destino)).isFalse();
    }

    @Test
    void listaDestinosPermitidosDeCadaOrigem() {
        assertThat(FluxoStatus.destinosPermitidos(ABERTO)).containsExactlyInAnyOrder(EM_ATENDIMENTO, FECHADO);
        assertThat(FluxoStatus.destinosPermitidos(EM_ATENDIMENTO))
                .containsExactlyInAnyOrder(AGUARDANDO_USUARIO, RESOLVIDO);
        assertThat(FluxoStatus.destinosPermitidos(FECHADO)).isEmpty();
    }
}
