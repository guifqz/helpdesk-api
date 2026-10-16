package dev.guifqz.helpdesk.web;

import dev.guifqz.helpdesk.domain.Categoria;
import dev.guifqz.helpdesk.domain.Prioridade;
import dev.guifqz.helpdesk.domain.StatusChamado;
import dev.guifqz.helpdesk.security.UsuarioAutenticado;
import dev.guifqz.helpdesk.service.ChamadoService;
import dev.guifqz.helpdesk.web.dto.AtribuicaoRequest;
import dev.guifqz.helpdesk.web.dto.ChamadoRequest;
import dev.guifqz.helpdesk.web.dto.ChamadoResponse;
import dev.guifqz.helpdesk.web.dto.PaginaResponse;
import dev.guifqz.helpdesk.web.dto.StatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chamados")
@Tag(name = "Chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    @Operation(summary = "Abre um chamado em nome do usuário autenticado")
    public ResponseEntity<ChamadoResponse> criar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChamadoRequest request) {
        ChamadoResponse criado = chamadoService.criar(UsuarioAutenticado.de(jwt), request);
        return ResponseEntity.created(URI.create("/api/chamados/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista chamados com paginação e filtros; solicitantes veem apenas os próprios")
    public PaginaResponse<ChamadoResponse> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Categoria categoria,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return chamadoService.listar(UsuarioAutenticado.de(jwt), status, prioridade, categoria, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um chamado pelo id")
    public ChamadoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return chamadoService.buscar(UsuarioAutenticado.de(jwt), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza título, descrição, categoria e prioridade (recalcula o SLA)")
    public ChamadoResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody ChamadoRequest request) {
        return chamadoService.atualizar(UsuarioAutenticado.de(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status respeitando a máquina de estados (TECNICO/ADMIN)")
    public ChamadoResponse alterarStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        return chamadoService.alterarStatus(UsuarioAutenticado.de(jwt), id, request.status());
    }

    @PatchMapping("/{id}/atribuir")
    @Operation(summary = "Atribui o chamado a um técnico (TECNICO/ADMIN)")
    public ChamadoResponse atribuir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody AtribuicaoRequest request) {
        return chamadoService.atribuir(UsuarioAutenticado.de(jwt), id, request.tecnicoId());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um chamado (ADMIN)")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        chamadoService.remover(UsuarioAutenticado.de(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
