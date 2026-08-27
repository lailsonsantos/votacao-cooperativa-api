package br.com.cooperativa.votacao.api.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cooperativa.votacao.api.ui.dto.AcaoTelaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AcaoTelaRequest")
class AcaoTelaRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Desserializa um JSON como o cliente enviaria.
     *
     * @param json corpo da requisição
     * @return o objeto preenchido
     * @throws Exception se o JSON for inválido
     */
    private AcaoTelaRequest de(String json) throws Exception {
        return mapper.readValue(json, AcaoTelaRequest.class);
    }

    @Test
    @DisplayName("aceita numero enviado como valor JSON")
    void inteiroComoNumero() throws Exception {
        assertThat(de("{\"duracaoMinutos\":5}").inteiro("duracaoMinutos")).isEqualTo(5);
    }

    @Test
    @DisplayName("aceita numero enviado como texto")
    void inteiroComoTexto() throws Exception {
        assertThat(de("{\"duracaoMinutos\":\"5\"}").inteiro("duracaoMinutos")).isEqualTo(5);
    }

    @Test
    @DisplayName("devolve nulo em vez de estourar quando o texto nao e numero")
    void inteiroInvalido() throws Exception {
        // Nulo faz o serviço aplicar o padrão; uma exceção aqui viraria 500 para
        // uma entrada que o usuário simplesmente digitou errado.
        assertThat(de("{\"duracaoMinutos\":\"abc\"}").inteiro("duracaoMinutos")).isNull();
    }

    @Test
    @DisplayName("devolve nulo para campo ausente ou vazio")
    void inteiroAusente() throws Exception {
        assertThat(de("{}").inteiro("duracaoMinutos")).isNull();
        assertThat(de("{\"duracaoMinutos\":\"\"}").inteiro("duracaoMinutos")).isNull();
        assertThat(de("{\"duracaoMinutos\":null}").inteiro("duracaoMinutos")).isNull();
    }

    @Test
    @DisplayName("trunca decimal ao ler como inteiro")
    void inteiroDecimal() throws Exception {
        assertThat(de("{\"duracaoMinutos\":5.9}").inteiro("duracaoMinutos")).isEqualTo(5);
    }

    @Test
    @DisplayName("remove espacos ao redor do texto")
    void textoComEspacos() throws Exception {
        assertThat(de("{\"cpf\":\"  19839091069  \"}").texto("cpf")).isEqualTo("19839091069");
    }

    @Test
    @DisplayName("texto so de espacos equivale a ausente")
    void textoEmBranco() throws Exception {
        // Distinguir "" de null aqui evitaria que o servidor aplicasse o padrão;
        // tratar ambos como ausente é o comportamento útil.
        assertThat(de("{\"cpf\":\"   \"}").texto("cpf")).isNull();
        assertThat(de("{}").texto("cpf")).isNull();
        assertThat(de("{\"cpf\":null}").texto("cpf")).isNull();
    }

    @Test
    @DisplayName("converte valor nao textual para texto")
    void textoDeNumero() throws Exception {
        assertThat(de("{\"opcao\":123}").texto("opcao")).isEqualTo("123");
    }

    @Test
    @DisplayName("aceita qualquer conjunto de chaves, como o Anexo 1 exige")
    void aceitaChavesArbitrarias() throws Exception {
        var acao = de("{\"campo1\":\"valor1\",\"campo2\":123,\"idCampoData\":\"01/01/2000\"}");

        assertThat(acao.getCampos())
                .containsEntry("campo1", "valor1")
                .containsEntry("campo2", 123)
                .containsEntry("idCampoData", "01/01/2000");
    }

    @Test
    @DisplayName("a fabrica de INPUT_DATA produz o item no formato do Anexo 1")
    void itemDeData() {
        var item =
                br.com.cooperativa.votacao.api.ui.dto.ItemTela.inputData(
                        "idCampoData", "Campo data", "01/01/2000");

        // O tipo faz parte do catálogo documentado no Anexo 1, ainda que nenhuma
        // tela atual o utilize; deixa-lo sem teste esconderia uma quebra futura.
        assertThat(item.tipo())
                .isEqualTo(br.com.cooperativa.votacao.api.ui.dto.enums.TipoItem.INPUT_DATA);
        assertThat(item.id()).isEqualTo("idCampoData");
        assertThat(item.valor()).isEqualTo("01/01/2000");
        assertThat(item.texto()).isNull();
    }

    @Test
    @DisplayName("expoe os campos como vista imutavel")
    void camposImutaveis() throws Exception {
        var campos = de("{\"cpf\":\"1\"}").getCampos();

        // Devolver a coleção interna deixaria qualquer consumidor alterar o
        // estado do objeto pelas costas de quem o construiu.
        assertThatThrownBy(() -> campos.put("outro", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
