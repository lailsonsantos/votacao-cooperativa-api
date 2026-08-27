package br.com.cooperativa.votacao.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
