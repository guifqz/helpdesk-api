package dev.guifqz.helpdesk.exception;

import dev.guifqz.helpdesk.domain.StatusChamado;

public class TransicaoInvalidaException extends RegraNegocioException {

    public TransicaoInvalidaException(StatusChamado origem, StatusChamado destino) {
        super("Transição de status inválida: %s -> %s".formatted(origem, destino));
    }
}
