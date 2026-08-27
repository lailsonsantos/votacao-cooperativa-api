package br.com.cooperativa.votacao.api.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cooperativa.votacao.IntegracaoTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("Contrato das telas (Anexo 1)")
class TelaContratoIT extends IntegracaoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    /**
     * Cadastra uma pauta pela propria camada de telas.
     *
     * @param titulo titulo da pauta
     * @return o identificador extraido da URL do botao devolvido
     * @throws Exception se a requisicao falhar
     */
    private UUID criarPautaPelaTela(String titulo) throws Exception {
        var corpo =
                mockMvc.perform(
                                post("/api/v1/telas/pautas")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"titulo":"%s","descricao":"Descricao da pauta"}
                                                """
                                                        .formatted(titulo)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        // A tela devolvida ja aponta para a acao de abrir sessao da pauta criada;
        // o identificador vem dali, exatamente como o cliente o obteria.
        var url = objectMapper.readTree(corpo).get("botaoOk").get("url").asText();
        var partes = url.split("/");
        return UUID.fromString(partes[partes.length - 2]);
    }

    @Test
    @DisplayName("menu e uma tela SELECAO com URLs absolutas configuraveis")
    void menu() throws Exception {
        mockMvc.perform(get("/api/v1/telas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.titulo").value("Assembleia Cooperativa"))
                .andExpect(jsonPath("$.itens", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.itens[0].texto").exists())
                // URL absoluta, montada a partir de app.callback.base-url.
                .andExpect(
                        jsonPath("$.itens[0].url")
                                .value(Matchers.startsWith("http://localhost:8080/api/v1/telas")))
                // SELECAO nao carrega botaoOk nem botaoCancelar; campos nulos sao
                // omitidos para que o JSON seja identico ao do Anexo 1.
                .andExpect(jsonPath("$.botaoOk").doesNotExist())
                .andExpect(jsonPath("$.botaoCancelar").doesNotExist());
    }

    @Test
    @DisplayName("formulario de nova pauta traz os tipos de item do Anexo 1")
    void formularioNovaPauta() throws Exception {
        mockMvc.perform(get("/api/v1/telas/pautas/nova"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[0].tipo").value("TEXTO"))
                .andExpect(jsonPath("$.itens[0].texto").exists())
                // Item TEXTO nao pode carregar "id": null.
                .andExpect(jsonPath("$.itens[0].id").doesNotExist())
                .andExpect(jsonPath("$.itens[1].tipo").value("INPUT_TEXTO"))
                .andExpect(jsonPath("$.itens[1].id").value("titulo"))
                .andExpect(jsonPath("$.itens[1].titulo").exists())
                .andExpect(jsonPath("$.botaoOk.texto").exists())
                .andExpect(jsonPath("$.botaoOk.url").exists())
                .andExpect(jsonPath("$.botaoCancelar.texto").value("Cancelar"));
    }

    @Test
    @DisplayName("pauta sem sessao oferece INPUT_NUMERO com a duracao padrao")
    void pautaSemSessao() throws Exception {
        var pautaId = criarPautaPelaTela("Pauta para abrir sessao");

        mockMvc.perform(get("/api/v1/telas/pautas/{id}", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(
                        jsonPath("$.itens[?(@.tipo=='INPUT_NUMERO')].id")
                                .value(Matchers.hasItem("duracaoMinutos")))
                .andExpect(
                        jsonPath("$.itens[?(@.id=='duracaoMinutos')].valor")
                                .value(Matchers.hasItem(1)))
                .andExpect(jsonPath("$.botaoOk.url").value(Matchers.endsWith("/sessao")));
    }

    @Test
    @DisplayName("fluxo de voto: abre sessao, identifica CPF e devolve SELECAO Sim/Nao")
    void fluxoDeVoto() throws Exception {
        var pautaId = criarPautaPelaTela("Pauta para votar");

        // Abrir sessao devolve diretamente a tela de identificacao: a navegacao
        // e dirigida pelo servidor, sem o cliente decidir para onde ir.
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(
                        jsonPath("$.itens[?(@.id=='cpf')].tipo")
                                .value(Matchers.hasItem("INPUT_TEXTO")));

        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos/identificacao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"19839091069\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.itens[0].texto").value("Sim"))
                // O body de cada item carrega tudo que a acao precisa, conforme
                // o Anexo 1: o cliente apenas reenvia o objeto.
                .andExpect(jsonPath("$.itens[0].body.opcao").value("SIM"))
                .andExpect(jsonPath("$.itens[0].body.cpf").value("19839091069"))
                .andExpect(jsonPath("$.itens[1].texto").value("Nao"))
                .andExpect(jsonPath("$.itens[1].body.opcao").value("NAO"));

        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"19839091069\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(
                        jsonPath("$.itens[*].texto")
                                .value(Matchers.hasItem(Matchers.containsString("Sim: 1"))));
    }

    @Test
    @DisplayName("erro de negocio vira tela legivel com HTTP 200, nao status cru")
    void erroViraTela() throws Exception {
        var pautaId = criarPautaPelaTela("Pauta sem sessao para erro");

        // A API REST devolveria 409 aqui. A camada de telas devolve 200 com uma
        // tela que o cliente sabe renderizar, porque ele nao interpreta status.
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos/identificacao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"19839091069\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.titulo").value("Sessao nao aberta"))
                .andExpect(jsonPath("$.botaoOk.texto").value("Voltar"));
    }

    @Test
    @DisplayName("recusa CPF invalido na tela, onde Bean Validation nao alcanca")
    void cpfInvalidoNaTela() throws Exception {
        var pautaId = criarPautaPelaTela("Pauta com cpf invalido na tela");

        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":10}"))
                .andExpect(status().isOk());

        // Aqui o corpo e um mapa aberto, nao da pra usar @CPF. Quem valida e o
        // objeto Cpf, e o erro vira tela.
        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos/identificacao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"11111111111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.titulo").value("CPF invalido"))
                .andExpect(
                        jsonPath("$.itens[0].texto").value(Matchers.containsString("111******11")));
    }

    @Test
    @DisplayName("impede oferecer voto a quem ja votou")
    void impedeVotoRepetidoNaTela() throws Exception {
        var pautaId = criarPautaPelaTela("Pauta com voto repetido");

        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/sessao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"duracaoMinutos\":10}"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"19839091069\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/telas/pautas/{id}/votos/identificacao", pautaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cpf\":\"19839091069\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(
                        jsonPath("$.itens[0].texto")
                                .value(Matchers.containsString("ja registrou")));
    }
}
