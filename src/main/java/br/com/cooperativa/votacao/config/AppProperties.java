package br.com.cooperativa.votacao.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(Callback callback, Sessao sessao) {
    /**
     * Configuração do host usado para montar as URLs absolutas das telas.
     *
     * @param baseUrl raiz pública da aplicação, sem barra final (ex.: {@code
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
     * Configuração padrão das sessões de votação.
     *
     * @param duracaoPadraoMinutos duração aplicada quando a chamada de abertura não informa uma
     *     duração. O enunciado define 1 minuto; o valor vive em configuração para não existir
     *     número mágico no código.
     */
    public record Sessao(@Positive int duracaoPadraoMinutos) {}
}
