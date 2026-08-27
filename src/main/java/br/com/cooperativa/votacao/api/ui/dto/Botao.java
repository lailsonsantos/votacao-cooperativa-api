package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Botao(String texto, String url, Map<String, Object> body) {

    /** Copia defensiva das coleções recebidas. */
    public Botao {
        // body nulo é significativo: distingue navegação de ação.
        body = body == null ? null : Map.copyOf(body);
    }

    /**
     * Cria um botão com corpo fixo.
     *
     * @param texto rótulo do botão
     * @param url destino absoluto da ação
     * @param body dados fixos a enviar
     * @return o botão correspondente
     */
    public static Botao de(String texto, String url, Map<String, Object> body) {
        return new Botao(texto, url, body);
    }

    /**
     * Cria um botão de navegação, sem corpo fixo.
     *
     * @param texto rótulo do botão
     * @param url destino absoluto
     * @return o botão correspondente
     */
    public static Botao navegacao(String texto, String url) {
        return new Botao(texto, url, null);
    }
}
