package dev.guifqz.helpdesk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(@NotBlank @Size(max = 2000) String texto) {
}
