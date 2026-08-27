package br.com.cooperativa.votacao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cooperativa.votacao.domain.model.Pagina;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes do calculo de paginacao.
 *
 * <p>A aritmetica de {@code totalPaginas} e {@code ultima} decide se o cliente continua paginando
 * ou para. Um erro de arredondamento faria a ultima pagina ser ignorada, escondendo pautas da
 * assembleia sem nenhum sinal de erro.
 */
@DisplayName("Pagina")
class PaginaTest {

    @ParameterizedTest(name = "total={0} tamanho={1} resulta em {2} paginas")
    @CsvSource({
        "0,  10, 0",
        "1,  10, 1",
        "10, 10, 1",
        // O caso que um arredondamento para baixo quebraria: sobra 1 elemento.
        "11, 10, 2",
        "19, 10, 2",
        "20, 10, 2",
        "21, 10, 3"
    })
    @DisplayName("arredonda o total de paginas para cima")
    void totalPaginas(long total, int tamanho, int esperado) {
        var pagina = new Pagina<>(List.of(), 0, tamanho, total);
        assertThat(pagina.totalPaginas()).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "pagina={0} de {1} elementos: ultima={2}")
    @CsvSource({"0, 25, false", "1, 25, false", "2, 25, true", "0, 0,  true", "0, 5,  true"})
    @DisplayName("identifica corretamente a ultima pagina")
    void ultima(int indice, long total, boolean esperado) {
        var pagina = new Pagina<>(List.of(), indice, 10, total);
        assertThat(pagina.ultima()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("nao divide por zero quando o tamanho e invalido")
    void tamanhoZero() {
        // Defesa contra um caminho que so surgiria por engano; o importante e nao
        // derrubar a requisicao com ArithmeticException.
        var pagina = new Pagina<>(List.of(), 0, 0, 50);
        assertThat(pagina.totalPaginas()).isZero();
    }

    @Test
    @DisplayName("converte o conteudo preservando os dados de paginacao")
    void mapear() {
        var original = new Pagina<>(List.of(1, 2, 3), 2, 3, 42);

        var convertida = original.mapear(n -> "n" + n);

        assertThat(convertida.conteudo()).containsExactly("n1", "n2", "n3");
        assertThat(convertida.pagina()).isEqualTo(2);
        assertThat(convertida.tamanho()).isEqualTo(3);
        assertThat(convertida.totalElementos()).isEqualTo(42);
    }

    @Test
    @DisplayName("conteudo nulo vira lista vazia, e nao NullPointerException")
    void conteudoNulo() {
        // Defesa da copia defensiva: List.copyOf recusa nulo, entao o caso e
        // tratado antes. Sem isso, uma porta mal implementada derrubaria a API.
        assertThat(new Pagina<String>(null, 0, 10, 0).conteudo()).isEmpty();
    }

    @Test
    @DisplayName("a lista devolvida e imutavel")
    void conteudoImutavel() {
        var pagina = new Pagina<>(new java.util.ArrayList<>(List.of("a")), 0, 10, 1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pagina.conteudo().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("reconhece pagina vazia")
    void vazia() {
        assertThat(new Pagina<>(List.of(), 0, 10, 0).vazia()).isTrue();
        assertThat(new Pagina<>(List.of("a"), 0, 10, 1).vazia()).isFalse();
    }
}
