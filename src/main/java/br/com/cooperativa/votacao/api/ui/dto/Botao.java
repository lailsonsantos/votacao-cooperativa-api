package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Botao de acao no rodape de uma tela {@link TipoTela#FORMULARIO}.
 *
 * <p>Ao ser acionado, o cliente envia {@code POST} para {@link #url()} com o
 * {@link #body()} acrescido dos valores digitados nos campos da tela, indexados
 * pelo {@code id} de cada um.
 *
 * @param texto rotulo do botao
 * @param url   destino absoluto da acao
 * @param body  dados fixos enviados na acao; omitido do JSON quando vazio
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Botao(String texto, String url, Map<String, Object> body) {
    /**
     * Cria um botao com corpo fixo.
     *
     * @param texto rotulo do botao
     * @param url   destino absoluto da acao
     * @param body  dados fixos a enviar
     * @return o botao correspondente
     */
    public static Botao de(String texto, String url, Map<String, Object> body) {
        return new Botao(texto, url, body);
    }

    /**
     * Cria um botao de navegacao, sem corpo fixo.
     *
     * <p>Usado em "Voltar" e "Cancelar", onde a acao e apenas ir para outra tela.
     *
     * @param texto rotulo do botao
     * @param url   destino absoluto
     * @return o botao correspondente
     */
    public static Botao navegacao(String texto, String url) {
        return new Botao(texto, url, null);
    }
}
