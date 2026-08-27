package br.com.cooperativa.votacao.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cooperativa.votacao.api.v1.dto.response.PaginaResponse;
import br.com.cooperativa.votacao.domain.model.Pagina;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaginaResponse")
class PaginaResponseTest {

    @Test
    @DisplayName("converte o conteudo e preserva os dados de paginacao")
    void converte() {
        var dominio = new Pagina<>(List.of(1, 2, 3), 1, 3, 10);

        var resposta = PaginaResponse.de(dominio, n -> "item" + n);

        assertThat(resposta.conteudo()).containsExactly("item1", "item2", "item3");
        assertThat(resposta.pagina()).isEqualTo(1);
        assertThat(resposta.tamanho()).isEqualTo(3);
        assertThat(resposta.totalElementos()).isEqualTo(10);
        assertThat(resposta.totalPaginas()).isEqualTo(4);
        assertThat(resposta.ultima()).isFalse();
    }

    @Test
    @DisplayName("marca a ultima pagina corretamente")
    void ultimaPagina() {
        var resposta = PaginaResponse.de(new Pagina<>(List.of("a"), 3, 3, 10), s -> s);

        assertThat(resposta.ultima()).isTrue();
    }

    @Test
    @DisplayName("conteudo nulo vira lista vazia")
    void conteudoNulo() {
        var resposta = new PaginaResponse<String>(null, 0, 10, 0, 0, true);

        assertThat(resposta.conteudo()).isEmpty();
    }

    @Test
    @DisplayName("lida com pagina vazia sem quebrar")
    void vazia() {
        var resposta =
                PaginaResponse.de(new Pagina<Integer>(List.of(), 0, 20, 0), Object::toString);

        assertThat(resposta.conteudo()).isEmpty();
        assertThat(resposta.totalElementos()).isZero();
        assertThat(resposta.ultima()).isTrue();
    }
}
