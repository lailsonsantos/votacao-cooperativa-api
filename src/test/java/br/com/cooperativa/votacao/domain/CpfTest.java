package br.com.cooperativa.votacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cooperativa.votacao.domain.exception.CpfInvalidoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do objeto de valor {@link Cpf}.
 *
 * <p>A validacao acontece antes de qualquer chamada remota, entao um defeito
 * aqui gastaria rede com numeros impossiveis e permitiria persistir votos com
 * identificador invalido.
 */
@DisplayName("Cpf")
class CpfTest {

    @ParameterizedTest
    @ValueSource(strings = {"19839091069", "198.390.910-69", "62289608068"})
    @DisplayName("aceita CPF valido com ou sem pontuacao")
    void aceitaCpfValido(String valor) {
        assertThat(Cpf.de(valor).numero()).hasSize(11).containsOnlyDigits();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11111111111", "00000000000", "12345678901", "123", "abcdefghijk"})
    @DisplayName("recusa CPF invalido, inclusive digitos repetidos")
    void recusaCpfInvalido(String valor) {
        assertThatThrownBy(() -> Cpf.de(valor)).isInstanceOf(CpfInvalidoException.class);
    }

    @Test
    @DisplayName("recusa valor nulo sem lancar NullPointerException")
    void recusaNulo() {
        assertThatThrownBy(() -> Cpf.de(null)).isInstanceOf(CpfInvalidoException.class);
    }

    @Test
    @DisplayName("mascara o CPF preservando apenas as bordas")
    void mascara() {
        assertThat(Cpf.de("19839091069").mascarado()).isEqualTo("198******69");
    }

    @Test
    @DisplayName("nao quebra ao mascarar valor curto ou nulo")
    void mascaraValorInesperado() {
        assertThat(Cpf.mascarar(null)).isNull();
        assertThat(Cpf.mascarar("123")).isEqualTo("123");
    }
}
