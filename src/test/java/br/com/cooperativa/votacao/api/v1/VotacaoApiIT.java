package br.com.cooperativa.votacao.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cooperativa.votacao.IntegracaoTest;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Testes de integração da API REST v1, ponta a ponta sobre PostgreSQL real. */
@DisplayName("API v1")
class VotacaoApiIT extends IntegracaoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VotoRepository votoRepository;

    /**
     * Cadastra uma pauta e devolve seu identificador.
     *
     * @param titulo titulo da pauta
     * @return o identificador da pauta criada
     * @throws Exception se a requisição falhar
     */
    private UUID criarPauta(String titulo) throws Exception {
        var resposta =
                mockMvc.perform(
                                post("/api/v1/pautas")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"titulo":"%s","descricao":"Descricao"}
                                                """
                                                        .formatted(titulo)))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return UUID.fromString(objectMapper.readTree(resposta).get("id").asText());
    }

    /**
     * Abre a sessão de uma pauta.
     *
     * @param pautaId identificador da pauta
     * @param minutos duração solicitada
     * @throws Exception se a requisição falhar
     */
    private void abrirSessao(UUID pautaId, int minutos) throws Exception {
        mockMvc.perform(
                        post("/api/v1/pautas/{id}/sessao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":%d}".formatted(minutos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    @DisplayName("percorre o fluxo completo: pauta, sessao, votos e resultado")
    void fluxoCompleto() throws Exception {
        var pautaId = criarPauta("Reforma do estatuto");
        abrirSessao(pautaId, 10);

        votar(pautaId, "19839091069", "SIM").andExpect(status().isCreated());
        votar(pautaId, "62289608068", "SIM").andExpect(status().isCreated());
        votar(pautaId, "11144477735", "NAO").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/pautas/{id}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votosSim").value(2))
                .andExpect(jsonPath("$.votosNao").value(1))
                .andExpect(jsonPath("$.totalVotos").value(3))
                .andExpect(jsonPath("$.resultado").value("APROVADA"))
                // A sessão segue aberta, então o resultado precisa vir marcado
                // como parcial: o número ainda pode mudar.
                .andExpect(jsonPath("$.parcial").value(true));
    }

    @Test
    @DisplayName("recusa o segundo voto do mesmo associado com 409 e ProblemDetail")
    void recusaVotoDuplicado() throws Exception {
        var pautaId = criarPauta("Pauta com voto duplicado");
        abrirSessao(pautaId, 10);

        votar(pautaId, "19839091069", "SIM").andExpect(status().isCreated());

        votar(pautaId, "19839091069", "NAO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Voto duplicado"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.correlationId").exists())
                // O CPF não pode voltar completo na mensagem de erro.
                .andExpect(
                        jsonPath("$.detail")
                                .value(org.hamcrest.Matchers.containsString("198******69")));
    }

    @Test
    @DisplayName("recusa abrir uma segunda sessao para a mesma pauta")
    void recusaSegundaSessao() throws Exception {
        var pautaId = criarPauta("Pauta com sessao unica");
        abrirSessao(pautaId, 5);

        mockMvc.perform(
                        post("/api/v1/pautas/{id}/sessao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Sessao ja aberta"));
    }

    @Test
    @DisplayName("aplica a duracao padrao de 1 minuto quando o corpo e omitido")
    void aplicaDuracaoPadrao() throws Exception {
        var pautaId = criarPauta("Pauta com duracao padrao");

        mockMvc.perform(post("/api/v1/pautas/{id}/sessao", pautaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                // 60 segundos exatos; a tolerância cobre o tempo de execução.
                .andExpect(
                        jsonPath("$.segundosRestantes")
                                .value(org.hamcrest.Matchers.lessThanOrEqualTo(60)));
    }

    @Test
    @DisplayName("recusa voto em pauta sem sessao aberta")
    void recusaVotoSemSessao() throws Exception {
        var pautaId = criarPauta("Pauta sem sessao");

        votar(pautaId, "19839091069", "SIM")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Sessao nao aberta"));
    }

    @Test
    @DisplayName("recusa CPF invalido com 400 antes de tocar o banco")
    void recusaCpfInvalido() throws Exception {
        var pautaId = criarPauta("Pauta com cpf invalido");
        abrirSessao(pautaId, 5);

        // 11111111111 tem onze dígitos mas não é CPF válido. O @CPF pega na borda
        // e a resposta diz qual campo falhou.
        votar(pautaId, "11111111111", "SIM")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisicao invalida"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(org.hamcrest.Matchers.containsString("associadoId")))
                .andExpect(
                        jsonPath("$.detail")
                                .value(org.hamcrest.Matchers.containsString("nao e valido")));
    }

    @Test
    @DisplayName("recusa opcao de voto fora do enum com 400, nao 500")
    void recusaOpcaoInvalida() throws Exception {
        var pautaId = criarPauta("Pauta com opcao invalida");
        abrirSessao(pautaId, 5);

        mockMvc.perform(
                        post("/api/v1/pautas/{id}/votos", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"associadoId":"19839091069","opcao":"TALVEZ"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("devolve 404 para pauta inexistente")
    void pautaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/pautas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    @Test
    @DisplayName("lista pautas paginadas com o total correto")
    void listaPaginada() throws Exception {
        criarPauta("Pauta para listagem A");
        criarPauta("Pauta para listagem B");

        mockMvc.perform(get("/api/v1/pautas").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(2))
                // O total vem de uma contagem própria, não do tamanho da fatia.
                .andExpect(
                        jsonPath("$.totalElementos")
                                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalPaginas").exists())
                .andExpect(jsonPath("$.ultima").exists());
    }

    @Test
    @DisplayName("detalha uma pauta existente")
    void detalhaPauta() throws Exception {
        var pautaId = criarPauta("Pauta para detalhe");

        mockMvc.perform(get("/api/v1/pautas/{id}", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pautaId.toString()))
                .andExpect(jsonPath("$.titulo").value("Pauta para detalhe"))
                .andExpect(jsonPath("$.criadaEm").exists());
    }

    @Test
    @DisplayName("rota inexistente devolve 404, nao 500")
    void rotaInexistente() throws Exception {
        // Sem tratador dedicado, isto caía no catch-all e virava 500: um erro do
        // cliente reportado como falha do servidor.
        mockMvc.perform(get("/api/v1/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Rota invalida"));
    }

    @Test
    @DisplayName("metodo nao suportado devolve 405, nao 500")
    void metodoNaoSuportado() throws Exception {
        mockMvc.perform(delete("/api/v1/pautas"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Rota invalida"));
    }

    @Test
    @DisplayName("limita o tamanho da pagina para impedir resposta ilimitada")
    void limitaTamanhoDaPagina() throws Exception {
        // Sem o @Max, size=999999 devolveria a base inteira.
        mockMvc.perform(get("/api/v1/pautas").param("size", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisicao invalida"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "nao pode passar de 100")));
    }

    @Test
    @DisplayName("recusa pagina negativa com 400, nao 500")
    void recusaPaginaNegativa() throws Exception {
        mockMvc.perform(get("/api/v1/pautas").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "nao pode ser negativa")));
    }

    @Test
    @DisplayName("recusa tamanho de pagina zero com 400, nao 500")
    void recusaTamanhoZero() throws Exception {
        mockMvc.perform(get("/api/v1/pautas").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.detail")
                                .value(org.hamcrest.Matchers.containsString("ao menos 1")));
    }

    @Test
    @DisplayName("garante um unico voto sob 200 requisicoes simultaneas do mesmo associado")
    void unicidadeSobConcorrencia() throws Exception {
        var pautaId = criarPauta("Pauta sob concorrencia");
        abrirSessao(pautaId, 10);

        var threads = 200;
        var largada = new CountDownLatch(1);
        var chegada = new CountDownLatch(threads);
        var sucessos = new AtomicInteger();
        var conflitos = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(32)) {
            for (int i = 0; i < threads; i++) {
                executor.submit(
                        () -> {
                            try {
                                // Todas as threads partem juntas: é a largada
                                // simultanea que cria a corrida real entre o
                                // SELECT e o INSERT que a constraint precisa vencer.
                                largada.await();
                                var status =
                                        mockMvc.perform(
                                                        post("/api/v1/pautas/{id}/votos", pautaId)
                                                                .contentType(
                                                                        MediaType.APPLICATION_JSON)
                                                                .content(
                                                                        """
                                                                        {"associadoId":"19839091069","opcao":"SIM"}
                                                                        """))
                                                .andReturn()
                                                .getResponse()
                                                .getStatus();

                                if (status == 201) {
                                    sucessos.incrementAndGet();
                                } else if (status == 409) {
                                    conflitos.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // Falhas inesperadas aparecem como divergência na
                                // soma verificada abaixo.
                            } finally {
                                chegada.countDown();
                            }
                        });
            }

            largada.countDown();
            assertThat(chegada.await(60, TimeUnit.SECONDS)).isTrue();
        }

        // Este é o teste que prova que a unicidade sobrevive a concorrência real.
        // Uma checagem "select antes de insert" falharia aqui de forma intermitente.
        assertThat(sucessos.get()).isEqualTo(1);
        assertThat(sucessos.get() + conflitos.get()).isEqualTo(threads);

        var sessaoId = buscarSessaoId(pautaId);
        assertThat(votoRepository.contarPorOpcao(sessaoId))
                .extracting("total")
                .isEqualTo(List.of(1L));
    }

    /**
     * Registra um voto pela API.
     *
     * @param pautaId identificador da pauta
     * @param cpf CPF do associado
     * @param opção opção escolhida
     * @return o resultado da requisição, para encadeamento de asserções
     * @throws Exception se a requisição falhar
     */
    private org.springframework.test.web.servlet.ResultActions votar(
            UUID pautaId, String cpf, String opcao) throws Exception {
        return mockMvc.perform(
                post("/api/v1/pautas/{id}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"associadoId":"%s","opcao":"%s"}
                                """
                                        .formatted(cpf, opcao)));
    }

    /**
     * Recupera o identificador da sessão de uma pauta pela API.
     *
     * @param pautaId identificador da pauta
     * @return o identificador da sessão
     * @throws Exception se a requisição falhar
     */
    private UUID buscarSessaoId(UUID pautaId) throws Exception {
        var corpo =
                mockMvc.perform(get("/api/v1/pautas/{id}/sessao", pautaId))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }
}
