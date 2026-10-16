package dev.guifqz.helpdesk.service;

import dev.guifqz.helpdesk.domain.Usuario;
import dev.guifqz.helpdesk.exception.CredenciaisInvalidasException;
import dev.guifqz.helpdesk.repository.UsuarioRepository;
import dev.guifqz.helpdesk.security.TokenService;
import dev.guifqz.helpdesk.web.dto.LoginRequest;
import dev.guifqz.helpdesk.web.dto.TokenResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public TokenResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return TokenResponse.bearer(tokenService.gerar(usuario), tokenService.segundosDeValidade());
    }
}
