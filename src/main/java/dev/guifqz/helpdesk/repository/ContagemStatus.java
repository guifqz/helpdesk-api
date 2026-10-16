package dev.guifqz.helpdesk.repository;

import dev.guifqz.helpdesk.domain.StatusChamado;

public record ContagemStatus(StatusChamado status, Long total) {
}
