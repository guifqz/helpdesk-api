package dev.guifqz.helpdesk.domain;

import java.util.EnumSet;
import java.util.Set;

public enum StatusChamado {
    ABERTO,
    EM_ATENDIMENTO,
    AGUARDANDO_USUARIO,
    RESOLVIDO,
    FECHADO;

    public static final Set<StatusChamado> FINALIZADOS = EnumSet.of(RESOLVIDO, FECHADO);

    public boolean finalizado() {
        return FINALIZADOS.contains(this);
    }
}
