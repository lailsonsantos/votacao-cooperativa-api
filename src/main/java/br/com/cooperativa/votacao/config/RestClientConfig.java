package br.com.cooperativa.votacao.config;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.web.client.RestClient;

/**
 * Constroi o cliente HTTP usado na integracao com o servico externo de CPF.
 *
 * <p>Optou-se por {@link RestClient} (Spring 6.1) em vez de {@code WebClient}
 * para nao trazer a stack reativa inteira para uma aplicacao MVC bloqueante que
 * faz uma unica chamada remota &mdash; seria complexidade sem contrapartida.
 */
@Configuration
public class RestClientConfig {

    /**
     * Cliente HTTP dedicado ao servico de consulta de associados.
     *
     * <p>Os timeouts sao curtos e explicitos de proposito: sem eles, o valor
     * padrao e "esperar para sempre", e uma indisponibilidade do terceiro
     * seguraria threads do servidor ate esgotar o pool durante uma assembleia.
     *
     * @param properties  configuracao do servico externo
     * @param customizers customizacoes aplicadas pelo Spring Boot (observabilidade)
     * @return o cliente configurado com URL base e timeouts
     */
    @Bean
    public RestClient userInfoRestClient(
            UserInfoProperties properties, java.util.List<RestClientCustomizer> customizers) {

        var settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                        .withReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        var builder =
                RestClient.builder()
                        .baseUrl(properties.baseUrl())
                        .requestFactory(ClientHttpRequestFactories.get(settings));

        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }
}
