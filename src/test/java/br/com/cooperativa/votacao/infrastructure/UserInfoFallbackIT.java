package br.com.cooperativa.votacao.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.infrastructure.integration.userinfo.UserInfoClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("local")
@DisplayName("Fallback da integração de CPF (Bonus 1)")
class UserInfoFallbackIT {

    private static WireMockServer wireMock;

    @Autowired private UserInfoClient client;

    @BeforeAll
    static void iniciarStub() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        // Serviço que nunca responde a tempo: reproduz exatamente a situação do
        // endpoint do enunciado, que está fora do ar.
        wireMock.stubFor(
                get(urlPathMatching("/users/.*"))
                        .willReturn(aResponse().withFixedDelay(5_000).withStatus(200)));
    }

    @AfterAll
    static void pararStub() {
        wireMock.stop();
    }

    /**
     * Aponta a aplicação para o stub e liga a integração neste teste.
     *
     * @param registry registro de propriedades dinâmicas do Spring Test
     */
    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("app.user-info.enabled", () -> true);
        registry.add("app.user-info.base-url", () -> "http://localhost:" + wireMock.port());
        registry.add("app.user-info.read-timeout-ms", () -> 300);
        registry.add("app.user-info.connect-timeout-ms", () -> 300);
        registry.add("app.user-info.fallback-permite-voto", () -> true);
    }

    @Test
    @DisplayName("libera o voto quando o serviço externo está indisponível")
    void aplicaFallbackConfigurado() {
        var resposta = client.consultar(Cpf.de("19839091069"));

        // A assembleia não pode ser interrompida pela indisponibilidade de um
        // terceiro. O comportamento é configurável; aqui verifica-se o padrão.
        assertThat(resposta).isPresent();
        assertThat(resposta.get().podeVotar()).isTrue();
    }
}
