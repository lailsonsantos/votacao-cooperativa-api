package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Opcao de uma tela do tipo {@link TipoTela#SELECAO}.
 *
 * <p>Funciona como um botao: ao ser tocada, o cliente envia {@code POST} para a {@code url} com o
 * {@code body} do item.
 *
 * @param texto rotulo da opcao
 * @param url destino absoluto da acao
 * @param body dados enviados ao acionar a opcao; omitido do JSON quando vazio
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemSelecao(String texto, String url, Map<String, Object> body) {

    /**
     * Copia defensiva das colecoes recebidas.
     *
     * <p>Um record e imutavel apenas na superficie: sem a copia, quem construiu a lista continua
     * podendo altera-la depois, e o objeto "imutavel" muda pelas costas de quem o recebeu. A
     * analise estatica sinaliza exatamente isso.
     */
    public ItemSelecao {
        // body nulo e significativo: distingue navegacao de acao.
        body = body == null ? null : Map.copyOf(body);
    }

    /**
     * Cria uma opcao com corpo.
     *
     * @param texto rotulo da opcao
     * @param url destino absoluto
     * @param body dados a enviar
     * @return a opcao correspondente
     */
    public static ItemSelecao de(String texto, String url, Map<String, Object> body) {
        return new ItemSelecao(texto, url, body);
    }

    /**
     * Cria uma opcao de navegacao, sem corpo.
     *
     * @param texto rotulo da opcao
     * @param url destino absoluto
     * @return a opcao correspondente
     */
    public static ItemSelecao navegacao(String texto, String url) {
        return new ItemSelecao(texto, url, null);
    }
}
