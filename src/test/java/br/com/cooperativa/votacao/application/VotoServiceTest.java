package br.com.cooperativa.votacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cooperativa.votacao.application.impl.VotoServiceImpl;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.model.Voto;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Testes das regras de registro de voto.
 *
 * <p>O {@link Clock} e fixo: simular o fim de uma sessao e trocar o instante do relogio, e nao
 * esperar tempo real. Sem isso, este arquivo levaria minutos para rodar e seria intermitente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VotoService")
class VotoServiceTest {

    private static final Instant ABERTURA = Instant.parse("2026-08-27T14:00:00Z");
    private static final Cpf CPF = Cpf.de("19839091069");

    @Mock private VotoRepository votoRepository;
    @Mock private SessaoVotacaoService sessaoService;
    @Mock private AssociadoValidator associadoValidator;

    private UUID pautaId;
    private SessaoVotacao sessao;

    @BeforeEach
    void prepararSessao() {
        var pauta = Pauta.criar("Reforma do estatuto", null, ABERTURA);
        pautaId = pauta.getId();
        sessao = SessaoVotacao.abrir(pauta, ABERTURA, Duration.ofMinutes(5));
    }

    /**
     * Monta o servico com o relogio posicionado em um instante especifico.
     *
     * @param agora instante que o servico enxergara como "agora"
     * @return o servico pronto para uso
     */
    private VotoService servicoEm(Instant agora) {
        return new VotoServiceImpl(
                votoRepository,
                sessaoService,
                associadoValidator,
                Clock.fixed(agora, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("registra o voto quando a sessao esta aberta")
    void registraVotoComSessaoAberta() {
        when(sessaoService.buscarObrigatoria(pautaId)).thenReturn(sessao);
        when(votoRepository.salvarEConfirmar(any(Voto.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        var voto = servicoEm(ABERTURA.plusSeconds(60)).registrar(pautaId, CPF, OpcaoVoto.SIM);

        assertThat(voto.getOpcao()).isEqualTo(OpcaoVoto.SIM);
        assertThat(voto.getAssociadoId()).isEqualTo(CPF.numero());
        verify(associadoValidator).validarPodeVotar(CPF);
    }

    @Test
    @DisplayName("recusa voto apos o fechamento da sessao")
    void recusaVotoComSessaoEncerrada() {
        when(sessaoService.buscarObrigatoria(pautaId)).thenReturn(sessao);

        var servico = servicoEm(ABERTURA.plusSeconds(301));

        assertThatThrownBy(() -> servico.registrar(pautaId, CPF, OpcaoVoto.SIM))
                .isInstanceOf(SessaoEncerradaException.class);

        // A verificacao externa de CPF nao deve ser acionada para uma sessao
        // encerrada: seria uma ida a rede garantidamente inutil.
        verify(associadoValidator, never()).validarPodeVotar(any());
        verify(votoRepository, never()).salvarEConfirmar(any());
    }

    @Test
    @DisplayName("recusa voto exatamente no instante do fechamento")
    void recusaVotoNoInstanteDoFechamento() {
        when(sessaoService.buscarObrigatoria(pautaId)).thenReturn(sessao);

        var servico = servicoEm(ABERTURA.plus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> servico.registrar(pautaId, CPF, OpcaoVoto.SIM))
                .isInstanceOf(SessaoEncerradaException.class);
    }

    @Test
    @DisplayName("traduz violacao de constraint em voto duplicado")
    void traduzViolacaoDeConstraint() {
        when(sessaoService.buscarObrigatoria(pautaId)).thenReturn(sessao);
        when(votoRepository.salvarEConfirmar(any(Voto.class)))
                .thenThrow(new DataIntegrityViolationException("uk_voto_sessao_associado"));

        var servico = servicoEm(ABERTURA.plusSeconds(60));

        assertThatThrownBy(() -> servico.registrar(pautaId, CPF, OpcaoVoto.SIM))
                .isInstanceOf(VotoDuplicadoException.class)
                // A mensagem devolvida ao cliente nao pode conter o CPF completo.
                .hasMessageContaining("198******69")
                .hasMessageNotContaining(CPF.numero());
    }
}
