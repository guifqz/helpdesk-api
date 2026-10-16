package dev.guifqz.helpdesk.web;

import dev.guifqz.helpdesk.security.UsuarioAutenticado;
import dev.guifqz.helpdesk.service.ComentarioService;
import dev.guifqz.helpdesk.web.dto.ComentarioRequest;
import dev.guifqz.helpdesk.web.dto.ComentarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chamados/{chamadoId}/comentarios")
@Tag(name = "Comentários")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona um comentário ao chamado")
    public ComentarioResponse adicionar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long chamadoId,
            @Valid @RequestBody ComentarioRequest request) {
        return comentarioService.adicionar(UsuarioAutenticado.de(jwt), chamadoId, request);
    }

    @GetMapping
    @Operation(summary = "Lista os comentários do chamado em ordem cronológica")
    public List<ComentarioResponse> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long chamadoId) {
        return comentarioService.listar(UsuarioAutenticado.de(jwt), chamadoId);
    }
}
