package br.com.cooperativa.votacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cooperativa.votacao.application.impl.SessaoVotacaoServiceImpl;
import br.com.cooperativa.votacao.config.AppProperties;
import br.com.cooperativa.votacao.domain.exception.SessaoJaAbertaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.repository.SessaoVotacaoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Testes das regras de abertura de sessão. */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessaoVotacaoService")
class SessaoVotacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-08-27T14:00:00Z");
    private static final int DURACAO_PADRAO = 1;

    @Mock private SessaoVotacaoRepository sessaoRepository;
    @Mock private PautaService pautaService;

    private SessaoVotacaoService servico;
    private Pauta pauta;

    @BeforeEach
    void prepararServico() {
        pauta = Pauta.criar("Reforma do estatuto", null, AGORA);

        var properties =
                new AppProperties(
                        new AppProperties.Callback("http://localhost:8080"),
                        new AppProperties.Sessao(DURACAO_PADRAO));

        servico =
                new SessaoVotacaoServiceImpl(
                        sessaoRepository,
                        pautaService,
                        properties,
                        Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("aplica a duracao padrao de 1 minuto quando nenhuma e informada")
    void aplicaDuracaoPadrao() {
        when(pautaService.buscar(pauta.getId())).thenReturn(pauta);
        when(sessaoRepository.existePorPauta(pauta.getId())).thenReturn(false);
        when(sessaoRepository.salvarEConfirmar(any(SessaoVotacao.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        var sessao = servico.abrir(pauta.getId(), null);

        assertThat(sessao.getFechamentoEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("aplica a duracao informada na chamada")
    void aplicaDuracaoInformada() {
        when(pautaService.buscar(pauta.getId())).thenReturn(pauta);
        when(sessaoRepository.existePorPauta(pauta.getId())).thenReturn(false);
        when(sessaoRepository.salvarEConfirmar(any(SessaoVotacao.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        var sessao = servico.abrir(pauta.getId(), 15);

        assertThat(sessao.getFechamentoEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("recusa abrir uma segunda sessao para a mesma pauta")
    void recusaSegundaSessao() {
        when(pautaService.buscar(pauta.getId())).thenReturn(pauta);
        when(sessaoRepository.existePorPauta(pauta.getId())).thenReturn(true);

        assertThatThrownBy(() -> servico.abrir(pauta.getId(), 5))
                .isInstanceOf(SessaoJaAbertaException.class);
    }

    @Test
    @DisplayName("traduz violacao de constraint em sessao ja aberta")
    void traduzViolacaoDeConstraint() {
        when(pautaService.buscar(pauta.getId())).thenReturn(pauta);
        // Checagem prévia passa: nenhuma sessão existia no instante da consulta.
        when(sessaoRepository.existePorPauta(pauta.getId())).thenReturn(false);
        // Entre a checagem e a gravação, outra requisição abriu a sessão. É a
        // constraint uk_sessão_pauta que decide, exatamente como no voto.
        when(sessaoRepository.salvarEConfirmar(any(SessaoVotacao.class)))
                .thenThrow(new DataIntegrityViolationException("uk_sessao_pauta"));

        assertThatThrownBy(() -> servico.abrir(pauta.getId(), 5))
                .isInstanceOf(SessaoJaAbertaException.class);
    }

    @Test
    @DisplayName("busca tolerante devolve vazio quando nao ha sessao")
    void buscaTolerante() {
        var pautaId = UUID.randomUUID();
        when(sessaoRepository.buscarPorPauta(pautaId)).thenReturn(Optional.empty());

        // Variante usada pelas telas, que precisam desenhar o caso "sem sessão"
        // sem tratar exceção.
        assertThat(servico.buscar(pautaId)).isEmpty();
    }

    @Test
    @DisplayName("exige sessao existente na consulta obrigatoria")
    void exigeSessaoExistente() {
        var pautaId = UUID.randomUUID();
        when(sessaoRepository.buscarPorPauta(pautaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.buscarObrigatoria(pautaId))
                .isInstanceOf(SessaoNaoAbertaException.class);
    }
}
