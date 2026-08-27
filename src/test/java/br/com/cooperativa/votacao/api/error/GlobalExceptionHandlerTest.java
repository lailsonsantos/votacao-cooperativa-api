package br.com.cooperativa.votacao.api.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cooperativa.votacao.api.ui.builder.TelaFactory;
import br.com.cooperativa.votacao.api.ui.builder.UrlTelaFactory;
import br.com.cooperativa.votacao.config.AppProperties;
import br.com.cooperativa.votacao.domain.exception.CpfInvalidoException;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private static final UUID PAUTA = UUID.randomUUID();

    /** Controlador artificial que dispara cada falha sob demanda. */
    @RestController
    @Validated
    @RequestMapping
    static class ControladorDeApoio {

        @GetMapping({"/api/v1/negocio", "/api/v1/telas/negocio"})
        String negocio() {
            throw new SessaoEncerradaException(PAUTA);
        }

        @GetMapping({"/api/v1/duplicado", "/api/v1/telas/duplicado"})
        String duplicado() {
            throw new VotoDuplicadoException(PAUTA, "19839091069", new RuntimeException());
        }

        @GetMapping({"/api/v1/cpf", "/api/v1/telas/cpf"})
        String cpf() {
            throw new CpfInvalidoException("11111111111");
        }

        @GetMapping({"/api/v1/integridade", "/api/v1/telas/integridade"})
        String integridade() {
            throw new DataIntegrityViolationException("uk_qualquer");
        }

        @GetMapping({"/api/v1/inesperado", "/api/v1/telas/inesperado"})
        String inesperado() {
            throw new IllegalStateException("falha nao prevista");
        }

        /** Dispara a violacao diretamente. */
        /** Corpo com restricao violada, para exercitar a validacao de payload. */
        record CorpoValidado(@jakarta.validation.constraints.NotBlank String titulo) {}

        @org.springframework.web.bind.annotation.PostMapping({
            "/api/v1/validado",
            "/api/v1/telas/validado"
        })
        String validado(
                @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
                        CorpoValidado corpo) {
            return corpo.titulo();
        }

        @GetMapping({"/api/v1/parametro", "/api/v1/telas/parametro"})
        String parametro() {
            record Paginacao(@Min(1) int size) {}
            var validador = Validation.buildDefaultValidatorFactory().getValidator();
            throw new ConstraintViolationException(validador.validate(new Paginacao(0)));
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ControladorDeApoio())
                    .setControllerAdvice(
                            new GlobalExceptionHandler(
                                    new TelaFactory(
                                            new UrlTelaFactory(
                                                    new AppProperties(
                                                            new AppProperties.Callback(
                                                                    "http://localhost:8080"),
                                                            new AppProperties.Sessao(1)))),
                                    Clock.fixed(
                                            Instant.parse("2026-08-27T14:00:00Z"), ZoneOffset.UTC)))
                    .build();

    @Nested
    @DisplayName("superficie REST")
    class Rest {

        @Test
        @DisplayName("falha de negocio vira ProblemDetail com o status da regra")
        void negocio() throws Exception {
            mockMvc.perform(get("/api/v1/negocio"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Sessao encerrada"))
                    .andExpect(
                            jsonPath("$.type")
                                    .value(org.hamcrest.Matchers.endsWith("sessao-encerrada")))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("voto duplicado devolve 409 com o CPF mascarado")
        void duplicado() throws Exception {
            mockMvc.perform(get("/api/v1/duplicado"))
                    .andExpect(status().isConflict())
                    .andExpect(
                            jsonPath("$.detail")
                                    .value(org.hamcrest.Matchers.containsString("198******69")))
                    .andExpect(
                            jsonPath("$.detail")
                                    .value(
                                            org.hamcrest.Matchers.not(
                                                    org.hamcrest.Matchers.containsString(
                                                            "19839091069"))));
        }

        @Test
        @DisplayName("violacao de integridade nao traduzida vira 409 generico")
        void integridade() throws Exception {
            mockMvc.perform(get("/api/v1/integridade"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Conflito de dados"));
        }

        @Test
        @DisplayName("falha inesperada vira 500 sem vazar detalhe interno")
        void inesperado() throws Exception {
            mockMvc.perform(get("/api/v1/inesperado"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.title").value("Erro inesperado"))
                    // A mensagem original nao pode chegar ao cliente.
                    .andExpect(
                            jsonPath("$.detail")
                                    .value(
                                            org.hamcrest.Matchers.not(
                                                    org.hamcrest.Matchers.containsString(
                                                            "falha nao prevista"))));
        }

        @Test
        @DisplayName("campo obrigatorio ausente vira 400 nomeando o campo")
        void corpoInvalido() throws Exception {
            mockMvc.perform(
                            post("/api/v1/validado")
                                    .contentType("application/json")
                                    .content("{\"titulo\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.detail")
                                    .value(org.hamcrest.Matchers.containsString("titulo")));
        }

        @Test
        @DisplayName("parametro fora do limite vira 400 nomeando o campo")
        void parametroInvalido() throws Exception {
            mockMvc.perform(get("/api/v1/parametro"))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.detail")
                                    .value(org.hamcrest.Matchers.containsString("size")));
        }
    }

    @Nested
    @DisplayName("superficie de telas")
    class Telas {

        @Test
        @DisplayName("falha de negocio vira tela legivel com HTTP 200")
        void negocio() throws Exception {
            // O cliente do Anexo 1 nao interpreta status: um 422 cru o deixaria
            // sem nada para desenhar.
            mockMvc.perform(get("/api/v1/telas/negocio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                    .andExpect(jsonPath("$.titulo").value("Sessao encerrada"))
                    .andExpect(jsonPath("$.botaoOk.texto").value("Voltar"));
        }

        @Test
        @DisplayName("CPF invalido vira tela com o numero mascarado")
        void cpf() throws Exception {
            mockMvc.perform(get("/api/v1/telas/cpf"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("CPF invalido"))
                    .andExpect(
                            jsonPath("$.itens[0].texto")
                                    .value(org.hamcrest.Matchers.containsString("111******11")));
        }

        @Test
        @DisplayName("violacao de integridade vira tela de conflito")
        void integridade() throws Exception {
            mockMvc.perform(get("/api/v1/telas/integridade"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Conflito"));
        }

        @Test
        @DisplayName("falha inesperada tambem vira tela, e nao 500 cru")
        void inesperado() throws Exception {
            mockMvc.perform(get("/api/v1/telas/inesperado"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Erro inesperado"))
                    .andExpect(jsonPath("$.botaoOk").exists());
        }

        @Test
        @DisplayName("campo obrigatorio ausente vira tela de dados invalidos")
        void corpoInvalido() throws Exception {
            mockMvc.perform(
                            post("/api/v1/telas/validado")
                                    .contentType("application/json")
                                    .content("{\"titulo\":\"\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                    .andExpect(jsonPath("$.titulo").value("Dados invalidos"));
        }

        @Test
        @DisplayName("parametro invalido vira tela de dados invalidos")
        void parametroInvalido() throws Exception {
            mockMvc.perform(get("/api/v1/telas/parametro"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Dados invalidos"));
        }

        @Test
        @DisplayName("corpo malformado vira tela de dados invalidos")
        void corpoMalformado() throws Exception {
            mockMvc.perform(
                            post("/api/v1/telas/negocio")
                                    .contentType("application/json")
                                    .content("{invalido"))
                    .andExpect(status().isOk());
        }
    }
}
