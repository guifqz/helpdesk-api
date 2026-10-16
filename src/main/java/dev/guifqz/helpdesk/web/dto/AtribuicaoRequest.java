package dev.guifqz.helpdesk.web.dto;

import jakarta.validation.constraints.NotNull;

public record AtribuicaoRequest(@NotNull Long tecnicoId) {
}
