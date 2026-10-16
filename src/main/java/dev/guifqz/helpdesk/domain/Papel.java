package dev.guifqz.helpdesk.domain;

public enum Papel {
    SOLICITANTE,
    TECNICO,
    ADMIN;

    public boolean atendeChamados() {
        return this == TECNICO || this == ADMIN;
    }
}
