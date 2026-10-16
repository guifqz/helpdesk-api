package dev.guifqz.helpdesk.domain;

import static dev.guifqz.helpdesk.domain.StatusChamado.ABERTO;
import static dev.guifqz.helpdesk.domain.StatusChamado.AGUARDANDO_USUARIO;
import static dev.guifqz.helpdesk.domain.StatusChamado.EM_ATENDIMENTO;
import static dev.guifqz.helpdesk.domain.StatusChamado.FECHADO;
import static dev.guifqz.helpdesk.domain.StatusChamado.RESOLVIDO;

import dev.guifqz.helpdesk.exception.TransicaoInvalidaException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class FluxoStatus {

    private static final Map<StatusChamado, Set<StatusChamado>> TRANSICOES = Map.of(
            ABERTO, EnumSet.of(EM_ATENDIMENTO, FECHADO),
            EM_ATENDIMENTO, EnumSet.of(AGUARDANDO_USUARIO, RESOLVIDO),
            AGUARDANDO_USUARIO, EnumSet.of(EM_ATENDIMENTO, FECHADO),
            RESOLVIDO, EnumSet.of(EM_ATENDIMENTO, FECHADO),
            FECHADO, EnumSet.noneOf(StatusChamado.class));

    private FluxoStatus() {
    }

    public static Set<StatusChamado> destinosPermitidos(StatusChamado origem) {
        return Set.copyOf(TRANSICOES.get(origem));
    }

    public static boolean permitida(StatusChamado origem, StatusChamado destino) {
        return TRANSICOES.get(origem).contains(destino);
    }

    public static void validar(StatusChamado origem, StatusChamado destino) {
        if (!permitida(origem, destino)) {
            throw new TransicaoInvalidaException(origem, destino);
        }
    }
}
