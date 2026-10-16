package dev.guifqz.helpdesk.repository;

import dev.guifqz.helpdesk.domain.Comentario;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    @EntityGraph(attributePaths = "autor")
    List<Comentario> findByChamadoIdOrderByCriadoEmAscIdAsc(Long chamadoId);
}
