package dev.guifqz.helpdesk.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ChamadoApiTest extends ApiTestSupport {

    @Test
    void solicitanteAbreChamadoComSlaDaPrioridade() throws Exception {
        String token = tokenSolicitante();

        MvcResult resultado = mockMvc.perform(post("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoChamado("Servidor ERP lento", "ERP", "CRITICA")))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.prioridade").value("CRITICA"))
                .andExpect(jsonPath("$.solicitante.nome").value("Usuario Demo"))
                .andExpect(jsonPath("$.tecnico").doesNotExist())
                .andExpect(jsonPath("$.slaVencido").value(false))
                .andReturn();

        JsonNode corpo = lerJson(resultado);
        Instant criadoEm = Instant.parse(corpo.get("criadoEm").asText());
        Instant prazoSla = Instant.parse(corpo.get("prazoSla").asText());
        assertThat(Duration.between(criadoEm, prazoSla)).isEqualTo(Duration.ofHours(4));
    }

    @Test
    void dadosInvalidosRetornam400ComErrosPorCampo() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenSolicitante()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descricao\":\"texto\",\"prioridade\":\"ALTA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo").exists())
                .andExpect(jsonPath("$.erros.categoria").exists());
    }

    @Test
    void categoriaDesconhecidaRetorna400() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenSolicitante()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoChamado("Teste", "IMPRESSORA", "ALTA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitanteListaApenasOsProprios() throws Exception {
        String solicitante = tokenSolicitante();
        String tecnico = tokenTecnico();
        criarChamado(solicitante, "Mouse quebrado", "HARDWARE", "BAIXA");
        criarChamado(solicitante, "Sem acesso ao ERP", "ACESSO", "ALTA");
        criarChamado(tecnico, "Atualizar antivirus", "SOFTWARE", "MEDIA");

        mockMvc.perform(get("/api/chamados").header(HttpHeaders.AUTHORIZATION, bearer(solicitante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get("/api/chamados").header(HttpHeaders.AUTHORIZATION, bearer(tecnico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
    }

    @Test
    void solicitanteNaoAcessaChamadoDeOutroUsuario() throws Exception {
        long id = criarChamado(tokenAdmin(), "Chamado do admin", "REDE", "MEDIA");

        mockMvc.perform(get("/api/chamados/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenSolicitante())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void tecnicoVeChamadoDeQualquerSolicitante() throws Exception {
        long id = criarChamado(tokenSolicitante(), "Monitor sem imagem", "HARDWARE", "MEDIA");

        mockMvc.perform(get("/api/chamados/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTecnico())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Monitor sem imagem"));
    }

    @Test
    void buscarChamadoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/chamados/{id}", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listagemAplicaFiltrosCombinados() throws Exception {
        String admin = tokenAdmin();
        criarChamado(admin, "VPN caiu", "REDE", "ALTA");
        criarChamado(admin, "Relatorio ERP", "ERP", "BAIXA");
        long terceiro = criarChamado(admin, "Wi-Fi lento", "REDE", "BAIXA");
        alterarStatus(tokenTecnico(), terceiro, "EM_ATENDIMENTO").andExpect(status().isOk());

        assertThat(totalNaListagem(admin, "categoria", "REDE")).isEqualTo(2);
        assertThat(totalNaListagem(admin, "prioridade", "BAIXA")).isEqualTo(2);
        assertThat(totalNaListagem(admin, "status", "EM_ATENDIMENTO")).isEqualTo(1);
        assertThat(totalNaListagem(admin, "status", "RESOLVIDO")).isZero();

        mockMvc.perform(get("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .param("categoria", "REDE")
                        .param("prioridade", "BAIXA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Wi-Fi lento"));
    }

    @Test
    void listagemEhPaginada() throws Exception {
        String admin = tokenAdmin();
        criarChamado(admin, "Chamado 1", "REDE", "BAIXA");
        criarChamado(admin, "Chamado 2", "REDE", "BAIXA");
        criarChamado(admin, "Chamado 3", "REDE", "BAIXA");

        mockMvc.perform(get("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .param("size", "2")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void filtroComStatusDesconhecidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin()))
                        .param("status", "INEXISTENTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tecnicoIniciaAtendimentoEAssumeOChamado() throws Exception {
        long id = criarChamado(tokenSolicitante(), "Notebook nao liga", "HARDWARE", "ALTA");

        alterarStatus(tokenTecnico(), id, "EM_ATENDIMENTO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.tecnico.nome").value("Tecnico Demo"));
    }

    @Test
    void solicitanteNaoAlteraStatus() throws Exception {
        String solicitante = tokenSolicitante();
        long id = criarChamado(solicitante, "Teclado com defeito", "HARDWARE", "BAIXA");

        alterarStatus(solicitante, id, "EM_ATENDIMENTO")
                .andExpect(status().isForbidden());
    }

    @Test
    void transicaoInvalidaRetorna422() throws Exception {
        long id = criarChamado(tokenSolicitante(), "Senha expirada", "ACESSO", "MEDIA");

        alterarStatus(tokenTecnico(), id, "RESOLVIDO")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void statusDesconhecidoNoCorpoRetorna400() throws Exception {
        long id = criarChamado(tokenSolicitante(), "Pasta compartilhada", "ACESSO", "BAIXA");

        alterarStatus(tokenTecnico(), id, "CANCELADO")
                .andExpect(status().isBadRequest());
    }

    @Test
    void cicloCompletoAteFecharEImpedeReabrirFechado() throws Exception {
        String tecnico = tokenTecnico();
        long id = criarChamado(tokenSolicitante(), "Instalar Office", "SOFTWARE", "MEDIA");

        alterarStatus(tecnico, id, "EM_ATENDIMENTO").andExpect(status().isOk());
        alterarStatus(tecnico, id, "AGUARDANDO_USUARIO").andExpect(status().isOk());
        alterarStatus(tecnico, id, "EM_ATENDIMENTO").andExpect(status().isOk());
        alterarStatus(tecnico, id, "RESOLVIDO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvidoEm").isNotEmpty());
        alterarStatus(tecnico, id, "FECHADO").andExpect(status().isOk());
        alterarStatus(tecnico, id, "EM_ATENDIMENTO").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminAtribuiChamadoAoTecnico() throws Exception {
        long id = criarChamado(tokenSolicitante(), "Trocar toner", "HARDWARE", "BAIXA");
        long tecnicoId = idUsuario(TECNICO);

        mockMvc.perform(patch("/api/chamados/{id}/atribuir", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":" + tecnicoId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnico.id").value(tecnicoId))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void naoEhPossivelAtribuirChamadoAUmSolicitante() throws Exception {
        long id = criarChamado(tokenSolicitante(), "Trocar toner", "HARDWARE", "BAIXA");
        long solicitanteId = idUsuario(SOLICITANTE);

        mockMvc.perform(patch("/api/chamados/{id}/atribuir", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":" + solicitanteId + "}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void solicitanteNaoAtribuiChamados() throws Exception {
        String solicitante = tokenSolicitante();
        long id = criarChamado(solicitante, "Trocar toner", "HARDWARE", "BAIXA");

        mockMvc.perform(patch("/api/chamados/{id}/atribuir", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":" + idUsuario(TECNICO) + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void alterarPrioridadeRecalculaOPrazoDeSla() throws Exception {
        String solicitante = tokenSolicitante();
        long id = criarChamado(solicitante, "Impressora offline", "HARDWARE", "BAIXA");

        MvcResult resultado = mockMvc.perform(put("/api/chamados/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoChamado("Impressora offline", "HARDWARE", "CRITICA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").value("CRITICA"))
                .andReturn();

        JsonNode corpo = lerJson(resultado);
        Duration prazo = Duration.between(
                Instant.parse(corpo.get("criadoEm").asText()),
                Instant.parse(corpo.get("prazoSla").asText()));
        assertThat(prazo).isEqualTo(Duration.ofHours(4));
    }

    @Test
    void solicitanteNaoAtualizaChamadoDeOutro() throws Exception {
        long id = criarChamado(tokenAdmin(), "Chamado do admin", "REDE", "MEDIA");

        mockMvc.perform(put("/api/chamados/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenSolicitante()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoChamado("Alterado", "REDE", "BAIXA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void somenteAdminRemoveChamado() throws Exception {
        long id = criarChamado(tokenSolicitante(), "Chamado duplicado", "REDE", "BAIXA");

        mockMvc.perform(delete("/api/chamados/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTecnico())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/chamados/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/chamados/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin())))
                .andExpect(status().isNotFound());
    }

    private int totalNaListagem(String token, String filtro, String valor) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param(filtro, valor))
                .andExpect(status().isOk())
                .andReturn();
        return lerJson(resultado).get("totalElementos").asInt();
    }
}
