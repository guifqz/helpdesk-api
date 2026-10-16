package dev.guifqz.helpdesk.domain;

import java.time.Instant;

public final class PoliticaSla {

    private PoliticaSla() {
    }

    public static Instant calcularPrazo(Instant inicio, Prioridade prioridade) {
        return inicio.plus(prioridade.prazoSla());
    }

    public static boolean vencido(Instant prazo, StatusChamado status, Instant agora) {
        return !status.finalizado() && agora.isAfter(prazo);
    }
}
