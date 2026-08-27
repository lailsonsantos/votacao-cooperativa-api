package br.com.cooperativa.votacao;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VotacaoApplication")
class VotacaoApplicationTest {

    @Test
    @DisplayName("a aplicacao sobe e encerra pelo metodo main")
    void sobePeloMain() {
        assertThatCode(
                        () ->
                                VotacaoApplication.main(
                                        new String[] {
                                            "--spring.profiles.active=local",
                                            "--server.port=0",
                                            "--spring.main.web-application-type=none"
                                        }))
                .doesNotThrowAnyException();
    }
}
