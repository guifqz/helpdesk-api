package dev.guifqz.helpdesk.security;

import dev.guifqz.helpdesk.domain.Papel;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public record UsuarioAutenticado(Long id, Papel papel) {

    public static UsuarioAutenticado de(Jwt jwt) {
        Number id = jwt.getClaim(TokenService.CLAIM_ID);
        List<String> papeis = jwt.getClaimAsStringList(TokenService.CLAIM_PAPEIS);
        return new UsuarioAutenticado(id.longValue(), Papel.valueOf(papeis.get(0)));
    }
}
