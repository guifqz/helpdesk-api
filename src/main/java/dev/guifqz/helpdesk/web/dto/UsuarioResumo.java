package dev.guifqz.helpdesk.web.dto;

import dev.guifqz.helpdesk.domain.Papel;
import dev.guifqz.helpdesk.domain.Usuario;

public record UsuarioResumo(Long id, String nome, Papel papel) {

    public static UsuarioResumo de(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getPapel());
    }
}
