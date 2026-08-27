package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemSelecao(String texto, String url, Map<String, Object> body) {

    /** Copia defensiva das colecoes recebidas. */
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
