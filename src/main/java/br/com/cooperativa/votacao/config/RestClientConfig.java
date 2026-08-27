package br.com.cooperativa.votacao.config;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

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
