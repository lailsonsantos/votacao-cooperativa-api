package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Botao(String texto, String url, Map<String, Object> body) {

    /** Copia defensiva das colecoes recebidas. */
    public Botao {
        // body nulo e significativo: distingue navegacao de acao.
        body = body == null ? null : Map.copyOf(body);
    }

    /**
     * Cria um botao com corpo fixo.
     *
     * @param texto rotulo do botao
     * @param url destino absoluto da acao
     * @param body dados fixos a enviar
     * @return o botao correspondente
     */
    public static Botao de(String texto, String url, Map<String, Object> body) {
        return new Botao(texto, url, body);
    }

    /**
     * Cria um botao de navegacao, sem corpo fixo.
     *
     * @param texto rotulo do botao
     * @param url destino absoluto
     * @return o botao correspondente
     */
    public static Botao navegacao(String texto, String url) {
        return new Botao(texto, url, null);
    }
}
