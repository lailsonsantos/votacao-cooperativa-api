package br.com.cooperativa.votacao.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cooperativa.votacao.application.impl.AssociadoValidatorImpl;
import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.infrastructure.integration.userinfo.UserInfoClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@DisplayName("UserInfoClient (Bonus 1)")
class UserInfoClientIT {

    private static WireMockServer wireMock;

    private UserInfoClient client;
    private UserInfoProperties properties;

    @BeforeAll
    static void iniciarStub() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void pararStub() {
        wireMock.stop();
    }

    @BeforeEach
    void prepararCliente() {
        wireMock.resetAll();
        properties =
                new UserInfoProperties("http://localhost:" + wireMock.port(), true, true, 500, 500);
        // Mesmos timeouts de produção: sem eles o cenário de indisponibilidade
        // nunca dispararia.
        var settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                        .withReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        var restClient =
                RestClient.builder()
                        .baseUrl(properties.baseUrl())
                        .requestFactory(ClientHttpRequestFactories.get(settings))
                        .build();
        client = new UserInfoClient(restClient, properties);
    }

    @Test
    @DisplayName("interpreta ABLE_TO_VOTE como associado habilitado")
    void habilitado() {
        wireMock.stubFor(
                get(urlPathEqualTo("/users/19839091069"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"status\":\"ABLE_TO_VOTE\"}")));

        var resposta = client.consultar(Cpf.de("19839091069"));

        assertThat(resposta).isPresent();
        assertThat(resposta.get().podeVotar()).isTrue();
    }

    @Test
    @DisplayName("interpreta UNABLE_TO_VOTE como associado impedido")
    void impedido() {
        wireMock.stubFor(
                get(urlPathEqualTo("/users/62289608068"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"status\":\"UNABLE_TO_VOTE\"}")));

        var validador = new AssociadoValidatorImpl(client, properties);

        assertThatThrownBy(() -> validador.validarPodeVotar(Cpf.de("62289608068")))
                .isInstanceOf(AssociadoNaoAutorizadoException.class);
    }

    @Test
    @DisplayName("trata 404 como CPF desconhecido, e não como falha de comunicação")
    void desconhecido() {
        wireMock.stubFor(
                get(urlPathEqualTo("/users/11144477735")).willReturn(aResponse().withStatus(404)));

        // 404 é uma resposta legítima do contrato. Traduzi-la em exceção
        // acionaria retry e circuit breaker para algo que nunca vai mudar.
        assertThat(client.consultar(Cpf.de("11144477735"))).isEmpty();
    }

    @Test
    @DisplayName("propaga a falha de rede para que retry e circuit breaker atuem")
    void indisponivelPropagaFalha() {
        wireMock.stubFor(
                get(urlPathEqualTo("/users/19839091069"))
                        .willReturn(aResponse().withFixedDelay(2_000).withStatus(200)));

        // Sem o proxy do Spring o fallback anotado não entra, então aqui a falha
        // propaga mesmo. O fallback real está em UserInfoFallbackIT.
        assertThatThrownBy(() -> client.consultar(Cpf.de("19839091069")))
                .isInstanceOf(ResourceAccessException.class);
    }
}
