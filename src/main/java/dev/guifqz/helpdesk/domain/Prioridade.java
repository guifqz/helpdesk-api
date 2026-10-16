package dev.guifqz.helpdesk.domain;

import java.time.Duration;

public enum Prioridade {
    BAIXA(Duration.ofHours(72)),
    MEDIA(Duration.ofHours(24)),
    ALTA(Duration.ofHours(8)),
    CRITICA(Duration.ofHours(4));

    private final Duration prazoSla;

    Prioridade(Duration prazoSla) {
        this.prazoSla = prazoSla;
    }

    public Duration prazoSla() {
        return prazoSla;
    }
}
