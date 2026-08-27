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

/**
 * Testes da integracao com o servico externo de CPF (Tarefa Bonus 1).
 *
 * <p>Toda a suite roda contra um WireMock local, jamais contra a rede real. Isso
 * e obrigatorio por dois motivos: o endpoint do enunciado esta fora do ar, e um
 * teste que depende de internet nao roda em CI de forma confiavel.
 *
 * <p>Cobre os quatro desfechos possiveis do contrato descrito no enunciado.
 */
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
                new UserInfoProperties(
                        "http://localhost:" + wireMock.port(), true, true, 500, 500);
        // Os mesmos timeouts de producao precisam valer aqui: sem eles, o cenario
        // de indisponibilidade nunca dispararia e o teste passaria por engano.
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
    @DisplayName("trata 404 como CPF desconhecido, e nao como falha de comunicacao")
    void desconhecido() {
        wireMock.stubFor(
                get(urlPathEqualTo("/users/11144477735"))
                        .willReturn(aResponse().withStatus(404)));

        // 404 e uma resposta legitima do contrato. Traduzi-la em excecao
        // acionaria retry e circuit breaker para algo que nunca vai mudar.
        assertThat(client.consultar(Cpf.de("11144477735"))).isEmpty();
    }

    @Test
    @DisplayName("propaga a falha de rede para que retry e circuit breaker atuem")
    void indisponivelPropagaFalha() {
        wireMock.stubFor(
                get(urlPathEqualTo("/users/19839091069"))
                        .willReturn(aResponse().withFixedDelay(2_000).withStatus(200)));

        // Aqui o cliente e instanciado sem o proxy do Spring, entao o fallback
        // anotado nao entra: o teste verifica o comportamento da camada crua, que
        // e propagar a falha para que retry e circuit breaker possam agir.
        // O efeito do fallback com a fiacao real e coberto por
        // UserInfoFallbackIT, que sobe o contexto da aplicacao.
        assertThatThrownBy(() -> client.consultar(Cpf.de("19839091069")))
                .isInstanceOf(ResourceAccessException.class);
    }
}
