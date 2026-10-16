package dev.guifqz.helpdesk.service;

import dev.guifqz.helpdesk.domain.Chamado;
import dev.guifqz.helpdesk.domain.Comentario;
import dev.guifqz.helpdesk.domain.StatusChamado;
import dev.guifqz.helpdesk.domain.Usuario;
import dev.guifqz.helpdesk.exception.RecursoNaoEncontradoException;
import dev.guifqz.helpdesk.exception.RegraNegocioException;
import dev.guifqz.helpdesk.repository.ComentarioRepository;
import dev.guifqz.helpdesk.repository.UsuarioRepository;
import dev.guifqz.helpdesk.security.UsuarioAutenticado;
import dev.guifqz.helpdesk.web.dto.ComentarioRequest;
import dev.guifqz.helpdesk.web.dto.ComentarioResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarios;
    private final UsuarioRepository usuarios;
    private final ChamadoService chamadoService;
    private final Clock clock;

    public ComentarioService(ComentarioRepository comentarios, UsuarioRepository usuarios,
            ChamadoService chamadoService, Clock clock) {
        this.comentarios = comentarios;
        this.usuarios = usuarios;
        this.chamadoService = chamadoService;
        this.clock = clock;
    }

    @Transactional
    public ComentarioResponse adicionar(UsuarioAutenticado atual, Long chamadoId, ComentarioRequest request) {
        Chamado chamado = chamadoService.carregarComAcesso(atual, chamadoId);
        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Chamado fechado não aceita novos comentários");
        }
        Usuario autor = usuarios.findById(atual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + atual.id() + " não encontrado"));
        Comentario comentario = comentarios.save(new Comentario(chamado, autor, request.texto(), Instant.now(clock)));
        return ComentarioResponse.de(comentario);
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> listar(UsuarioAutenticado atual, Long chamadoId) {
        chamadoService.carregarComAcesso(atual, chamadoId);
        return comentarios.findByChamadoIdOrderByCriadoEmAscIdAsc(chamadoId).stream()
                .map(ComentarioResponse::de)
                .toList();
    }
}
