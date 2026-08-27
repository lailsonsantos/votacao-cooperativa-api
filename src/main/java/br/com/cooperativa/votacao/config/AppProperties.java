package br.com.cooperativa.votacao.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de negocio da aplicacao, prefixadas por {@code app}.
 *
 * <p>Todos os valores sao externalizados por variavel de ambiente. Em especial, {@code
 * app.callback.base-url} atende a dica explicita do enunciado de deixar o dominio das URLs de
 * callback alteravel por configuracao, para que a mesma imagem funcione em emulador, dispositivo
 * fisico e nuvem sem recompilar.
 *
 * @param callback configuracao das URLs devolvidas nas telas
 * @param sessao configuracao padrao das sessoes de votacao
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(Callback callback, Sessao sessao) {
    /**
     * Configuracao do host usado para montar as URLs absolutas das telas.
     *
     * @param baseUrl raiz publica da aplicacao, sem barra final (ex.: {@code
     *     https://votacao-cooperativa-api.onrender.com})
     */
    public record Callback(@NotBlank String baseUrl) {
        /**
         * Devolve a URL base normalizada, sem barra final.
         *
         * <p>Normalizar aqui evita URLs com barra dupla quando o operador configura a variavel de
         * ambiente com {@code /} no fim &mdash; um erro comum que so apareceria em producao.
         *
         * @return a URL base sem barra ao final
         */
        public String baseUrlNormalizada() {
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }
    }

    /**
     * Configuracao padrao das sessoes de votacao.
     *
     * @param duracaoPadraoMinutos duracao aplicada quando a chamada de abertura nao informa uma
     *     duracao. O enunciado define 1 minuto; o valor vive em configuracao para nao existir
     *     numero magico no codigo.
     */
    public record Sessao(@Positive int duracaoPadraoMinutos) {}
}
