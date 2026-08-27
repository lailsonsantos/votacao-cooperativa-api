package br.com.cooperativa.votacao.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cooperativa.votacao.application.impl.AssociadoValidatorImpl;
import br.com.cooperativa.votacao.application.port.ConsultaAptidaoParaVotar;
import br.com.cooperativa.votacao.application.port.ConsultaAptidaoParaVotar.AptidaoParaVotar;
import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testes da verificação do direito de voto (Tarefa Bonus 1). */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssociadoValidator")
class AssociadoValidatorTest {

    private static final Cpf CPF = Cpf.de("19839091069");

    @Mock private ConsultaAptidaoParaVotar client;

    /**
     * Monta o validador com a integração ligada ou desligada.
     *
     * @param habilitada se a consulta remota deve acontecer
     * @return o validador configurado
     */
    private AssociadoValidator validador(boolean habilitada) {
        var properties = new UserInfoProperties("http://stub", habilitada, true, 2000, 3000);
        return new AssociadoValidatorImpl(client, properties);
    }

    @Test
    @DisplayName("libera o voto quando o serviço responde ABLE_TO_VOTE")
    void liberaQuandoHabilitado() {
        when(client.consultar(CPF)).thenReturn(Optional.of(new AptidaoParaVotar(true)));

        assertThatCode(() -> validador(true).validarPodeVotar(CPF)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloqueia o voto quando o serviço responde UNABLE_TO_VOTE")
    void bloqueiaQuandoImpedido() {
        when(client.consultar(CPF)).thenReturn(Optional.of(new AptidaoParaVotar(false)));

        assertThatThrownBy(() -> validador(true).validarPodeVotar(CPF))
                .isInstanceOf(AssociadoNaoAutorizadoException.class)
                .hasMessageContaining("não está habilitado");
    }

    @Test
    @DisplayName("bloqueia o voto quando o CPF é desconhecido (404 no serviço)")
    void bloqueiaQuandoDesconhecido() {
        when(client.consultar(CPF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validador(true).validarPodeVotar(CPF))
                .isInstanceOf(AssociadoNaoAutorizadoException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("não consulta o serviço quando a integração está desligada")
    void naoConsultaQuandoDesligado() {
        assertThatCode(() -> validador(false).validarPodeVotar(CPF)).doesNotThrowAnyException();

        // Este é o caminho que permite executar e avaliar a aplicação com o
        // serviço externo do enunciado fora do ar.
        verify(client, never()).consultar(CPF);
    }
}
