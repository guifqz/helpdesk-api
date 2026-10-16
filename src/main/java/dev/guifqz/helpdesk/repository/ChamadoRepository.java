package dev.guifqz.helpdesk.repository;

import dev.guifqz.helpdesk.domain.Chamado;
import dev.guifqz.helpdesk.domain.StatusChamado;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {

    @Override
    @EntityGraph(attributePaths = {"solicitante", "tecnico"})
    Page<Chamado> findAll(Specification<Chamado> spec, Pageable pageable);

    long countByPrazoSlaBeforeAndStatusNotIn(Instant limite, Collection<StatusChamado> status);

    @Query("select new dev.guifqz.helpdesk.repository.ContagemStatus(c.status, count(c)) "
            + "from Chamado c group by c.status")
    List<ContagemStatus> contarPorStatus();

    @Query("select new dev.guifqz.helpdesk.repository.TempoResolucao(c.criadoEm, c.resolvidoEm) "
            + "from Chamado c where c.resolvidoEm is not null")
    List<TempoResolucao> temposDeResolucao();
}
