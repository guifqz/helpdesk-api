package dev.guifqz.helpdesk.web.dto;

import dev.guifqz.helpdesk.domain.Categoria;
import dev.guifqz.helpdesk.domain.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
        @NotBlank @Size(max = 160) String titulo,
        @NotBlank @Size(max = 4000) String descricao,
        @NotNull Categoria categoria,
        @NotNull Prioridade prioridade) {
}
