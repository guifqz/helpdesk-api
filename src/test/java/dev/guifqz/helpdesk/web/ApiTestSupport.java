package dev.guifqz.helpdesk.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.guifqz.helpdesk.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class ApiTestSupport {

    protected static final String ADMIN = "admin@helpdesk.local";
    protected static final String ADMIN_SENHA = "admin123";
    protected static final String TECNICO = "tecnico@helpdesk.local";
    protected static final String TECNICO_SENHA = "tecnico123";
    protected static final String SOLICITANTE = "usuario@helpdesk.local";
    protected static final String SOLICITANTE_SENHA = "usuario123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UsuarioRepository usuarios;

    @Autowired
    protected JdbcTemplate jdbc;

    @PersistenceContext
    protected EntityManager entityManager;

    protected String tokenAdmin() throws Exception {
        return login(ADMIN, ADMIN_SENHA);
    }

    protected String tokenTecnico() throws Exception {
        return login(TECNICO, TECNICO_SENHA);
    }

    protected String tokenSolicitante() throws Exception {
        return login(SOLICITANTE, SOLICITANTE_SENHA);
    }

    protected String login(String email, String senha) throws Exception {
        String corpo = "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha);
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andReturn();
        return lerJson(resultado).get("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected long criarChamado(String token, String titulo, String categoria, String prioridade) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/chamados")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoChamado(titulo, categoria, prioridade)))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(resultado).get("id").asLong();
    }

    protected ResultActions alterarStatus(String token, long chamadoId, String novoStatus) throws Exception {
        return mockMvc.perform(patch("/api/chamados/{id}/status", chamadoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"%s\"}".formatted(novoStatus)));
    }

    protected static String corpoChamado(String titulo, String categoria, String prioridade) {
        return "{\"titulo\":\"%s\",\"descricao\":\"Descricao do problema\",\"categoria\":\"%s\",\"prioridade\":\"%s\"}"
                .formatted(titulo, categoria, prioridade);
    }

    protected JsonNode lerJson(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected long idUsuario(String email) {
        return usuarios.findByEmail(email).orElseThrow().getId();
    }

    protected void atualizarSql(String sql, Object... argumentos) {
        entityManager.flush();
        jdbc.update(sql, argumentos);
        entityManager.clear();
    }
}
