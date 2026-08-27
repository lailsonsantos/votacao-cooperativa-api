package br.com.cooperativa.votacao;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste de fumaca do ponto de entrada.
 *
 * <p>Verifica que a aplicacao sobe pelo mesmo caminho que a plataforma usa em producao: {@code
 * main}, e nao o contexto montado pelo Spring Test. Sao coisas diferentes — um erro em {@code
 * main}, como um perfil padrao errado, passaria despercebido por qualquer {@code @SpringBootTest}.
 *
 * <p>Usa o perfil {@code local}, com H2 em memoria, e uma porta aleatoria para nao disputar a 8080
 * com nada que ja esteja rodando.
 */
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
