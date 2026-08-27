package br.com.cooperativa.votacao.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.exception.CpfInvalidoException;
import br.com.cooperativa.votacao.domain.exception.NegocioException;
import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.SessaoJaAbertaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

@DisplayName("MapeadorDeStatus")
class MapeadorDeStatusTest {

    /**
     * Cada excecao de negocio com o status que deve produzir.
     *
     * @return os casos a verificar
     */
    static Stream<Arguments> excecoes() {
        var id = UUID.randomUUID();
        return Stream.of(
                Arguments.of(new RecursoNaoEncontradoException("Pauta", id), HttpStatus.NOT_FOUND),
                Arguments.of(new SessaoJaAbertaException(id), HttpStatus.CONFLICT),
                Arguments.of(new SessaoNaoAbertaException(id), HttpStatus.CONFLICT),
                Arguments.of(
                        new VotoDuplicadoException(id, "19839091069", new RuntimeException()),
                        HttpStatus.CONFLICT),
                Arguments.of(new SessaoEncerradaException(id), HttpStatus.UNPROCESSABLE_ENTITY),
                Arguments.of(
                        new AssociadoNaoAutorizadoException("impedido"),
                        HttpStatus.UNPROCESSABLE_ENTITY),
                Arguments.of(new CpfInvalidoException("111"), HttpStatus.BAD_REQUEST));
    }

    @ParameterizedTest(name = "produz {1}")
    @MethodSource("excecoes")
    @DisplayName("cada excecao de negocio produz o status correspondente")
    void mapeia(NegocioException excecao, HttpStatus esperado) {
        assertThat(MapeadorDeStatus.de(excecao.getTipo())).isEqualTo(esperado);
    }

    @ParameterizedTest
    @EnumSource(TipoErro.class)
    @DisplayName("toda natureza declarada tem status proprio, nunca 500")
    void nenhumaNaturezaSemMapeamento(TipoErro tipo) {
        // Uma natureza nova sem entrada na tabela cairia em 500 silenciosamente;
        // este teste faz a omissao aparecer no build.
        assertThat(MapeadorDeStatus.de(tipo)).isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("cada excecao carrega titulo e codigo estaveis")
    void tituloECodigo() {
        var e =
                new VotoDuplicadoException(
                        UUID.randomUUID(), "19839091069", new RuntimeException());

        assertThat(e.getTitulo()).isEqualTo("Voto duplicado");
        assertThat(e.getCodigo()).isEqualTo("voto-duplicado");
        // O codigo vai para o campo type do ProblemDetail e e contrato publico:
        // muda-lo quebraria clientes que reagem a tipos especificos de erro.
        assertThat(e.getTipo()).isEqualTo(TipoErro.CONFLITO);
    }

    @Test
    @DisplayName("titulos das demais excecoes")
    void titulos() {
        var id = UUID.randomUUID();
        assertThat(new RecursoNaoEncontradoException("Pauta", id).getTitulo())
                .isEqualTo("Recurso nao encontrado");
        assertThat(new SessaoJaAbertaException(id).getTitulo()).isEqualTo("Sessao ja aberta");
        assertThat(new SessaoNaoAbertaException(id).getTitulo()).isEqualTo("Sessao nao aberta");
        assertThat(new SessaoEncerradaException(id).getTitulo()).isEqualTo("Sessao encerrada");
        assertThat(new CpfInvalidoException("111").getTitulo()).isEqualTo("CPF invalido");
        assertThat(new AssociadoNaoAutorizadoException("x").getTitulo())
                .isEqualTo("Associado nao autorizado a votar");
    }
}
