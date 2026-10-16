package dev.guifqz.helpdesk.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class ComentarioDashboardApiTest extends ApiTestSupport {

    @Test
    void comentariosSaoListadosEmOrdemCronologica() throws Exception {
        String solicitante = tokenSolicitante();
        String tecnico = tokenTecnico();
        long id = criarChamado(solicitante, "Outlook travando", "SOFTWARE", "MEDIA");

        comentar(solicitante, id, "Acontece desde ontem").andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor.nome").value("Usuario Demo"));
        comentar(tecnico, id, "Vou reiniciar o perfil").andExpect(status().isCreated());

        mockMvc.perform(get("/api/chamados/{id}/comentarios", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].texto").value("Acontece desde ontem"))
                .andExpect(jsonPath("$[1].autor.nome").value("Tecnico Demo"));
    }

    @Test
    void comentarioVazioRetorna400() throws Exception {
        String solicitante = tokenSolicitante();
        long id = criarChamado(solicitante, "Outlook travando", "SOFTWARE", "MEDIA");

        comentar(solicitante, id, "   ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.texto").exists());
    }

    @Test
    void solicitanteNaoComentaNemLeComentariosDeChamadoAlheio() throws Exception {
        long id = criarChamado(tokenAdmin(), "Chamado do admin", "REDE", "MEDIA");
        String solicitante = tokenSolicitante();

        comentar(solicitante, id, "Posso ajudar?").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/chamados/{id}/comentarios", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante)))
                .andExpect(status().isForbidden());
    }

    @Test
    void chamadoFechadoNaoAceitaComentarios() throws Exception {
        String tecnico = tokenTecnico();
        long id = criarChamado(tokenSolicitante(), "Cadastro duplicado", "ERP", "BAIXA");
        alterarStatus(tecnico, id, "FECHADO").andExpect(status().isOk());

        comentar(tecnico, id, "Tarde demais").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void dashboardNegadoParaSolicitanteEParaAnonimo() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenSolicitante())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardVazioTemContagensZeradas() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/dashboard/resumo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTecnico())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChamados").value(0))
                .andExpect(jsonPath("$.porStatus.ABERTO").value(0))
                .andExpect(jsonPath("$.porStatus.FECHADO").value(0))
                .andExpect(jsonPath("$.slaVencido").value(0))
                .andReturn();

        assertThat(lerJson(resultado).get("tempoMedioResolucaoHoras").isNull()).isTrue();
    }

    @Test
    void dashboardContaChamadosPorStatus() throws Exception {
        String admin = tokenAdmin();
        String tecnico = tokenTecnico();
        criarChamado(admin, "Chamado A", "REDE", "BAIXA");
        criarChamado(admin, "Chamado B", "REDE", "BAIXA");
        long emAtendimento = criarChamado(admin, "Chamado C", "REDE", "BAIXA");
        alterarStatus(tecnico, emAtendimento, "EM_ATENDIMENTO").andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/resumo").header(HttpHeaders.AUTHORIZATION, bearer(tecnico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalChamados").value(3))
                .andExpect(jsonPath("$.porStatus.ABERTO").value(2))
                .andExpect(jsonPath("$.porStatus.EM_ATENDIMENTO").value(1));
    }

    @Test
    void dashboardContaApenasChamadosAbertosComSlaVencido() throws Exception {
        String admin = tokenAdmin();
        long vencido = criarChamado(admin, "Vencido", "REDE", "CRITICA");
        long resolvido = criarChamado(admin, "Resolvido fora do prazo", "REDE", "CRITICA");
        criarChamado(admin, "Dentro do prazo", "REDE", "BAIXA");
        alterarStatus(admin, resolvido, "EM_ATENDIMENTO").andExpect(status().isOk());
        alterarStatus(admin, resolvido, "RESOLVIDO").andExpect(status().isOk());
        atualizarSql("update chamado set prazo_sla = ? where id in (?, ?)",
                OffsetDateTime.now().minusDays(1), vencido, resolvido);

        mockMvc.perform(get("/api/dashboard/resumo").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slaVencido").value(1));
        mockMvc.perform(get("/api/chamados/{id}", vencido).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.slaVencido").value(true));
        mockMvc.perform(get("/api/chamados/{id}", resolvido).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.slaVencido").value(false));
    }

    @Test
    void dashboardCalculaTempoMedioDeResolucao() throws Exception {
        String admin = tokenAdmin();
        long id = criarChamado(admin, "Resolvido em duas horas", "SOFTWARE", "MEDIA");
        atualizarSql("update chamado set criado_em = ? where id = ?", OffsetDateTime.now().minusHours(2), id);
        alterarStatus(admin, id, "EM_ATENDIMENTO").andExpect(status().isOk());
        alterarStatus(admin, id, "RESOLVIDO").andExpect(status().isOk());

        MvcResult resultado = mockMvc.perform(get("/api/dashboard/resumo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode media = lerJson(resultado).get("tempoMedioResolucaoHoras");
        assertThat(media.asDouble()).isBetween(1.9, 2.1);
    }

    private ResultActions comentar(String token, long chamadoId, String texto) throws Exception {
        return mockMvc.perform(post("/api/chamados/{id}/comentarios", chamadoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"" + texto + "\"}"));
    }
}
