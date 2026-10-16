package dev.guifqz.helpdesk.service;

import dev.guifqz.helpdesk.domain.Categoria;
import dev.guifqz.helpdesk.domain.Chamado;
import dev.guifqz.helpdesk.domain.Papel;
import dev.guifqz.helpdesk.domain.Prioridade;
import dev.guifqz.helpdesk.domain.StatusChamado;
import dev.guifqz.helpdesk.domain.Usuario;
import dev.guifqz.helpdesk.exception.AcessoNegadoException;
import dev.guifqz.helpdesk.exception.RecursoNaoEncontradoException;
import dev.guifqz.helpdesk.exception.RegraNegocioException;
import dev.guifqz.helpdesk.repository.ChamadoRepository;
import dev.guifqz.helpdesk.repository.UsuarioRepository;
import dev.guifqz.helpdesk.security.UsuarioAutenticado;
import dev.guifqz.helpdesk.web.dto.ChamadoRequest;
import dev.guifqz.helpdesk.web.dto.ChamadoResponse;
import dev.guifqz.helpdesk.web.dto.PaginaResponse;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChamadoService {

    private final ChamadoRepository chamados;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public ChamadoService(ChamadoRepository chamados, UsuarioRepository usuarios, Clock clock) {
        this.chamados = chamados;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Transactional
    public ChamadoResponse criar(UsuarioAutenticado atual, ChamadoRequest request) {
        Usuario solicitante = buscarUsuario(atual.id());
        Chamado chamado = new Chamado(request.titulo(), request.descricao(), request.categoria(),
                request.prioridade(), solicitante, agora());
        return responder(chamados.save(chamado));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResponse> listar(UsuarioAutenticado atual, StatusChamado status,
            Prioridade prioridade, Categoria categoria, Pageable pageable) {
        Long solicitanteId = atual.papel() == Papel.SOLICITANTE ? atual.id() : null;
        Page<ChamadoResponse> pagina = chamados
                .findAll(filtro(solicitanteId, status, prioridade, categoria), pageable)
                .map(this::responder);
        return PaginaResponse.de(pagina);
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(UsuarioAutenticado atual, Long id) {
        return responder(carregarComAcesso(atual, id));
    }

    @Transactional
    public ChamadoResponse atualizar(UsuarioAutenticado atual, Long id, ChamadoRequest request) {
        Chamado chamado = carregarComAcesso(atual, id);
        chamado.atualizarDados(request.titulo(), request.descricao(), request.categoria(),
                request.prioridade(), agora());
        return responder(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(UsuarioAutenticado atual, Long id, StatusChamado novoStatus) {
        Chamado chamado = carregarComAcesso(atual, id);
        Instant agora = agora();
        chamado.alterarStatus(novoStatus, agora);
        if (novoStatus == StatusChamado.EM_ATENDIMENTO && chamado.getTecnico() == null) {
            chamado.atribuir(buscarUsuario(atual.id()), agora);
        }
        return responder(chamado);
    }

    @Transactional
    public ChamadoResponse atribuir(UsuarioAutenticado atual, Long id, Long tecnicoId) {
        Chamado chamado = carregarComAcesso(atual, id);
        Usuario tecnico = buscarUsuario(tecnicoId);
        if (!tecnico.getPapel().atendeChamados()) {
            throw new RegraNegocioException("O usuário informado não é técnico nem administrador");
        }
        chamado.atribuir(tecnico, agora());
        return responder(chamado);
    }

    @Transactional
    public void remover(UsuarioAutenticado atual, Long id) {
        chamados.delete(carregarComAcesso(atual, id));
    }

    Chamado carregarComAcesso(UsuarioAutenticado atual, Long id) {
        Chamado chamado = chamados.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + id + " não encontrado"));
        boolean alheio = !chamado.getSolicitante().getId().equals(atual.id());
        if (atual.papel() == Papel.SOLICITANTE && alheio) {
            throw new AcessoNegadoException("Você não tem acesso a este chamado");
        }
        return chamado;
    }

    private Usuario buscarUsuario(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado"));
    }

    private Specification<Chamado> filtro(Long solicitanteId, StatusChamado status,
            Prioridade prioridade, Categoria categoria) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (solicitanteId != null) {
                predicados.add(cb.equal(root.get("solicitante").get("id"), solicitanteId));
            }
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (prioridade != null) {
                predicados.add(cb.equal(root.get("prioridade"), prioridade));
            }
            if (categoria != null) {
                predicados.add(cb.equal(root.get("categoria"), categoria));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private ChamadoResponse responder(Chamado chamado) {
        return ChamadoResponse.de(chamado, agora());
    }

    private Instant agora() {
        return Instant.now(clock);
    }
}
