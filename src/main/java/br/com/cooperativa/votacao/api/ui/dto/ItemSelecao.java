package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemSelecao(String texto, String url, Map<String, Object> body) {

    /** Copia defensiva das coleções recebidas. */
    public ItemSelecao {
        // body nulo é significativo: distingue navegação de ação.
        body = body == null ? null : Map.copyOf(body);
    }

    /**
     * Cria uma opção com corpo.
     *
     * @param texto rótulo da opção
     * @param url destino absoluto
     * @param body dados a enviar
     * @return a opção correspondente
     */
    public static ItemSelecao de(String texto, String url, Map<String, Object> body) {
        return new ItemSelecao(texto, url, body);
    }

    /**
     * Cria uma opção de navegação, sem corpo.
     *
     * @param texto rótulo da opção
     * @param url destino absoluto
     * @return a opção correspondente
     */
    public static ItemSelecao navegacao(String texto, String url) {
        return new ItemSelecao(texto, url, null);
    }
}
