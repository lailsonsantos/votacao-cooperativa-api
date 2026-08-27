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

/**
 * Verifica o fallback da integracao de CPF com a fiacao real do Spring.
 *
 * <p>O fallback do Resilience4j so existe atraves do proxy criado pelo contexto: um cliente
 * instanciado com {@code new} ignora a anotacao. Por isso este cenario sobe a aplicacao, em vez de
 * montar o objeto na mao &mdash; caso contrario o teste passaria mesmo que a anotacao fosse
 * removida por engano.
 *
 * <p>Usa o perfil {@code local} (H2 em memoria) para nao depender de Docker: o objeto do teste e a
 * resiliencia, nao o banco.
 */
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("Fallback da integracao de CPF (Bonus 1)")
class UserInfoFallbackIT {

    private static WireMockServer wireMock;

    @Autowired private UserInfoClient client;

    @BeforeAll
    static void iniciarStub() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        // Servico que nunca responde a tempo: reproduz exatamente a situacao do
        // endpoint do enunciado, que esta fora do ar.
        wireMock.stubFor(
                get(urlPathMatching("/users/.*"))
                        .willReturn(aResponse().withFixedDelay(5_000).withStatus(200)));
    }

    @AfterAll
    static void pararStub() {
        wireMock.stop();
    }

    /**
     * Aponta a aplicacao para o stub e liga a integracao neste teste.
     *
     * @param registry registro de propriedades dinamicas do Spring Test
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
    @DisplayName("libera o voto quando o servico externo esta indisponivel")
    void aplicaFallbackConfigurado() {
        var resposta = client.consultar(Cpf.de("19839091069"));

        // A assembleia nao pode ser interrompida pela indisponibilidade de um
        // terceiro. O comportamento e configuravel; aqui verifica-se o padrao.
        assertThat(resposta).isPresent();
        assertThat(resposta.get().podeVotar()).isTrue();
    }
}
