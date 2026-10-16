package dev.guifqz.helpdesk.domain;

import dev.guifqz.helpdesk.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chamado")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusChamado status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    @Column(name = "prazo_sla", nullable = false)
    private Instant prazoSla;

    protected Chamado() {
    }

    public Chamado(String titulo, String descricao, Categoria categoria, Prioridade prioridade,
                   Usuario solicitante, Instant agora) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.solicitante = solicitante;
        this.status = StatusChamado.ABERTO;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        this.prazoSla = PoliticaSla.calcularPrazo(agora, prioridade);
    }

    public void alterarStatus(StatusChamado novoStatus, Instant agora) {
        FluxoStatus.validar(status, novoStatus);
        if (novoStatus == StatusChamado.RESOLVIDO) {
            resolvidoEm = agora;
        } else if (novoStatus == StatusChamado.EM_ATENDIMENTO) {
            resolvidoEm = null;
        }
        status = novoStatus;
        atualizadoEm = agora;
    }

    public void atribuir(Usuario novoTecnico, Instant agora) {
        if (status == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Chamado fechado não pode ser atribuído");
        }
        tecnico = novoTecnico;
        atualizadoEm = agora;
    }

    public void atualizarDados(String titulo, String descricao, Categoria categoria,
                               Prioridade prioridade, Instant agora) {
        if (status == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Chamado fechado não pode ser alterado");
        }
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.prazoSla = PoliticaSla.calcularPrazo(criadoEm, prioridade);
        this.atualizadoEm = agora;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Instant getResolvidoEm() {
        return resolvidoEm;
    }

    public Instant getPrazoSla() {
        return prazoSla;
    }
}
