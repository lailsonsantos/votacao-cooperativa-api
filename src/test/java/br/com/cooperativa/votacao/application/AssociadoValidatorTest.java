package br.com.cooperativa.votacao.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.application.port.ConsultaAptidaoParaVotar;
import br.com.cooperativa.votacao.application.port.ConsultaAptidaoParaVotar.AptidaoParaVotar;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes da verificacao do direito de voto (Tarefa Bonus 1).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssociadoValidator")
class AssociadoValidatorTest {

    private static final Cpf CPF = Cpf.de("19839091069");

    @Mock private ConsultaAptidaoParaVotar client;

    /**
     * Monta o validador com a integracao ligada ou desligada.
     *
     * @param habilitada se a consulta remota deve acontecer
     * @return o validador configurado
     */
    private AssociadoValidator validador(boolean habilitada) {
        var properties =
                new UserInfoProperties("http://stub", habilitada, true, 2000, 3000);
        return new AssociadoValidator(client, properties);
    }

    @Test
    @DisplayName("libera o voto quando o servico responde ABLE_TO_VOTE")
    void liberaQuandoHabilitado() {
        when(client.consultar(CPF))
                .thenReturn(Optional.of(new AptidaoParaVotar(true)));

        assertThatCode(() -> validador(true).validarPodeVotar(CPF)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloqueia o voto quando o servico responde UNABLE_TO_VOTE")
    void bloqueiaQuandoImpedido() {
        when(client.consultar(CPF))
                .thenReturn(Optional.of(new AptidaoParaVotar(false)));

        assertThatThrownBy(() -> validador(true).validarPodeVotar(CPF))
                .isInstanceOf(AssociadoNaoAutorizadoException.class)
                .hasMessageContaining("nao esta habilitado");
    }

    @Test
    @DisplayName("bloqueia o voto quando o CPF e desconhecido (404 no servico)")
    void bloqueiaQuandoDesconhecido() {
        when(client.consultar(CPF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validador(true).validarPodeVotar(CPF))
                .isInstanceOf(AssociadoNaoAutorizadoException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    @DisplayName("nao consulta o servico quando a integracao esta desligada")
    void naoConsultaQuandoDesligado() {
        assertThatCode(() -> validador(false).validarPodeVotar(CPF)).doesNotThrowAnyException();

        // Este e o caminho que permite executar e avaliar a aplicacao com o
        // servico externo do enunciado fora do ar.
        verify(client, never()).consultar(CPF);
    }
}
