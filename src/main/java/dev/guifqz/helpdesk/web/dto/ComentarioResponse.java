package dev.guifqz.helpdesk.web.dto;

import dev.guifqz.helpdesk.domain.Comentario;
import java.time.Instant;

public record ComentarioResponse(Long id, UsuarioResumo autor, String texto, Instant criadoEm) {

    public static ComentarioResponse de(Comentario comentario) {
        return new ComentarioResponse(
                comentario.getId(),
                UsuarioResumo.de(comentario.getAutor()),
                comentario.getTexto(),
                comentario.getCriadoEm());
    }
}
