package dev.guifqz.helpdesk.web.dto;

import dev.guifqz.helpdesk.domain.StatusChamado;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull StatusChamado status) {
}
