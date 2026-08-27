package br.com.cooperativa.votacao.api.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cooperativa.votacao.IntegracaoTest;
import br.com.cooperativa.votacao.RelogioDeTeste;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TelaEstadosIT.RelogioControlavel.class)
@DisplayName("Telas por estado da sessao")
class TelaEstadosIT extends IntegracaoTest {

    /** Substitui o relógio da aplicação por um que o teste consegue adiantar. */
    @TestConfiguration
    static class RelogioControlavel {

        /**
         * Relógio controlável, com precedência sobre o de sistema.
         *
         * @return o relógio de teste
         */
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RelogioDeTeste relogio;
    @Autowired private Clock clockInjetado;

    @BeforeEach
    void reiniciarRelogio() {
        relogio.reiniciar();
    }

    /**
     * Cria uma pauta pela camada de telas.
     *
     * @param titulo titulo da pauta
     * @return o identificador da pauta criada
     * @throws Exception se a requisição falhar
     */
    private UUID criarPauta(String titulo) throws Exception {
        var corpo =
                mockMvc.perform(
                                post("/api/v1/telas/pautas")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"titulo\":\"%s\"}".formatted(titulo)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var url = objectMapper.readTree(corpo).get("botaoOk").get("url").asText();
        var partes = url.split("/");
        return UUID.fromString(partes[partes.length - 2]);
    }

    @Test
    @DisplayName("o relogio controlavel realmente substitui o da aplicacao")
    void relogioSubstituido() {
        org.assertj.core.api.Assertions.assertThat(clockInjetado).isSameAs(relogio);
    }

    @Test
    @DisplayName("sem sessao, a tela oferece a abertura")
    void semSessao() throws Exception {
        var id = criarPauta("Pauta sem sessao");

        mockMvc.perform(get("/api/v1/telas/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.botaoOk.texto").value("Abrir sessao"));
    }

    @Test
    @DisplayName("com sessao aberta, a mesma URL passa a pedir o CPF")
    void sessaoAberta() throws Exception {
        var id = criarPauta("Pauta com sessao aberta");
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":10}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/telas/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(
                        jsonPath("$.itens[?(@.id=='cpf')].tipo")
                                .value(Matchers.hasItem("INPUT_TEXTO")))
                .andExpect(jsonPath("$.botaoOk.texto").value("Continuar"));
    }

    @Test
    @DisplayName("depois do prazo, a mesma URL passa a mostrar o resultado")
    void sessaoEncerrada() throws Exception {
        var id = criarPauta("Pauta que vai encerrar");
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"19839091069\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isOk());

        // Adianta o relógio em vez de esperar: o status da sessão é derivado dele.
        relogio.avancar(Duration.ofMinutes(2));

        mockMvc.perform(get("/api/v1/telas/pautas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(
                        jsonPath("$.itens[*].texto")
                                .value(
                                        Matchers.hasItem(
                                                Matchers.containsString("Resultado final"))))
                .andExpect(
                        jsonPath("$.itens[*].texto")
                                .value(Matchers.hasItem(Matchers.containsString("Sim: 1"))));
    }

    @Test
    @DisplayName("identificar-se apos o encerramento vira tela de erro")
    void identificacaoAposEncerramento() throws Exception {
        var id = criarPauta("Pauta encerrada para identificacao");
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":1}"))
                .andExpect(status().isOk());

        relogio.avancar(Duration.ofMinutes(2));

        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos/identificacao", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"19839091069\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Sessao encerrada"));
    }

    @Test
    @DisplayName("tela de resultado responde diretamente pela propria URL")
    void telaDeResultado() throws Exception {
        var id = criarPauta("Pauta para resultado direto");
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":10}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/telas/pautas/{id}/resultado", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(
                        jsonPath("$.itens[*].texto")
                                .value(Matchers.hasItem(Matchers.containsString("parcial"))));
    }

    @Test
    @DisplayName("lista de pautas responde pela camada de telas")
    void listaDePautas() throws Exception {
        criarPauta("Pauta na lista");

        mockMvc.perform(get("/api/v1/telas/pautas").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens").isNotEmpty());
    }

    @Test
    @DisplayName("corpo invalido na camada de telas vira tela, e nao status cru")
    void corpoInvalidoViraTela() throws Exception {
        var id = criarPauta("Pauta com corpo invalido");
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{quebrado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.titulo").value("Dados invalidos"));
    }
}
