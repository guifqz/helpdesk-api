package dev.guifqz.helpdesk.security;

import dev.guifqz.helpdesk.config.JwtProperties;
import dev.guifqz.helpdesk.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    static final String CLAIM_ID = "uid";
    static final String CLAIM_PAPEIS = "roles";

    private final JwtEncoder encoder;
    private final JwtProperties propriedades;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, JwtProperties propriedades, Clock clock) {
        this.encoder = encoder;
        this.propriedades = propriedades;
        this.clock = clock;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("helpdesk-api")
                .subject(usuario.getEmail())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.expiracao()))
                .claim(CLAIM_ID, usuario.getId())
                .claim(CLAIM_PAPEIS, List.of(usuario.getPapel().name()))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }

    public long segundosDeValidade() {
        return propriedades.expiracao().toSeconds();
    }
}
