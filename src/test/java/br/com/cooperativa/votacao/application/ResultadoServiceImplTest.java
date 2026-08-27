package br.com.cooperativa.votacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cooperativa.votacao.application.impl.ResultadoServiceImpl;
import br.com.cooperativa.votacao.config.CacheConfig;
import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import br.com.cooperativa.votacao.domain.enums.ResultadoApuracao;
import br.com.cooperativa.votacao.domain.enums.StatusSessao;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.repository.ContagemVotos;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResultadoServiceImpl")
class ResultadoServiceImplTest {

    private static final Instant ABERTURA = Instant.parse("2026-08-27T14:00:00Z");

    @Mock private VotoRepository votoRepository;
    @Mock private SessaoVotacaoService sessaoService;

    private ConcurrentMapCacheManager cacheManager;
    private Pauta pauta;
    private SessaoVotacao sessao;

    @BeforeEach
    void preparar() {
        cacheManager = new ConcurrentMapCacheManager(CacheConfig.CACHE_RESULTADO);
        pauta = Pauta.criar("Reforma do estatuto", null, ABERTURA);
        sessao = SessaoVotacao.abrir(pauta, ABERTURA, Duration.ofMinutes(5));
    }

    /**
     * Monta o servico com o relogio posicionado em um instante especifico.
     *
     * @param agora instante que o servico enxergara como atual
     * @return o servico pronto para uso
     */
    private ResultadoServiceImpl servicoEm(Instant agora) {
        return new ResultadoServiceImpl(
                votoRepository, sessaoService, cacheManager, Clock.fixed(agora, ZoneOffset.UTC));
    }

    /**
     * Cria uma linha de contagem como a consulta agregada devolveria.
     *
     * @param opcao opcao votada
     * @param total quantidade de votos
     * @return a projecao correspondente
     */
    private ContagemVotos contagem(OpcaoVoto opcao, long total) {
        return new ContagemVotos() {
            @Override
            public OpcaoVoto getOpcao() {
                return opcao;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    @DisplayName("apura sessao aberta e marca o resultado como parcial")
    void sessaoAberta() {
        when(sessaoService.buscarObrigatoria(pauta.getId())).thenReturn(sessao);
        when(votoRepository.contarPorOpcao(sessao.getId()))
                .thenReturn(List.of(contagem(OpcaoVoto.SIM, 3), contagem(OpcaoVoto.NAO, 1)));

        var resultado = servicoEm(ABERTURA.plusSeconds(60)).apurar(pauta.getId());

        assertThat(resultado.status()).isEqualTo(StatusSessao.ABERTA);
        assertThat(resultado.parcial()).isTrue();
        assertThat(resultado.votosSim()).isEqualTo(3);
        assertThat(resultado.resultado()).isEqualTo(ResultadoApuracao.APROVADA);
    }

    @Test
    @DisplayName("nao cacheia enquanto a sessao esta aberta")
    void naoCacheiaSessaoAberta() {
        when(sessaoService.buscarObrigatoria(pauta.getId())).thenReturn(sessao);
        when(votoRepository.contarPorOpcao(sessao.getId()))
                .thenReturn(List.of(contagem(OpcaoVoto.SIM, 1)));

        var servico = servicoEm(ABERTURA.plusSeconds(60));
        servico.apurar(pauta.getId());
        servico.apurar(pauta.getId());

        // Cachear aqui congelaria o placar durante a votacao.
        verify(votoRepository, times(2)).contarPorOpcao(sessao.getId());
        assertThat(cacheManager.getCache(CacheConfig.CACHE_RESULTADO).get(pauta.getId())).isNull();
    }

    @Test
    @DisplayName("cacheia o resultado depois do fechamento e reaproveita")
    void cacheiaSessaoFechada() {
        when(sessaoService.buscarObrigatoria(pauta.getId())).thenReturn(sessao);
        when(votoRepository.contarPorOpcao(sessao.getId()))
                .thenReturn(List.of(contagem(OpcaoVoto.SIM, 2), contagem(OpcaoVoto.NAO, 5)));

        var servico = servicoEm(ABERTURA.plusSeconds(600));
        var primeiro = servico.apurar(pauta.getId());
        var segundo = servico.apurar(pauta.getId());

        // Uma unica consulta para duas apuracoes: o resultado fechado e imutavel.
        verify(votoRepository, times(1)).contarPorOpcao(sessao.getId());
        assertThat(primeiro).isEqualTo(segundo);
        assertThat(primeiro.resultado()).isEqualTo(ResultadoApuracao.REPROVADA);
        assertThat(primeiro.parcial()).isFalse();
    }

    @Test
    @DisplayName("funciona mesmo sem cache configurado")
    void semCacheDisponivel() {
        when(sessaoService.buscarObrigatoria(pauta.getId())).thenReturn(sessao);
        when(votoRepository.contarPorOpcao(sessao.getId()))
                .thenReturn(List.of(contagem(OpcaoVoto.SIM, 1)));

        // Cache ausente nao pode derrubar a apuracao: e otimizacao, nao requisito.
        var semCache =
                new ResultadoServiceImpl(
                        votoRepository,
                        sessaoService,
                        new ConcurrentMapCacheManager("outro"),
                        Clock.fixed(ABERTURA.plusSeconds(600), ZoneOffset.UTC));

        assertThat(semCache.apurar(pauta.getId()).votosSim()).isEqualTo(1);
    }

    @Test
    @DisplayName("sessao sem nenhum voto resulta em SEM_VOTOS")
    void semVotos() {
        when(sessaoService.buscarObrigatoria(pauta.getId())).thenReturn(sessao);
        when(votoRepository.contarPorOpcao(sessao.getId())).thenReturn(List.of());

        var resultado = servicoEm(ABERTURA.plusSeconds(600)).apurar(pauta.getId());

        assertThat(resultado.resultado()).isEqualTo(ResultadoApuracao.SEM_VOTOS);
        assertThat(resultado.totalVotos()).isZero();
    }

    @Test
    @DisplayName("nao consulta o repositorio quando o resultado ja esta em cache")
    void reaproveitaCacheEntreInstancias() {
        when(sessaoService.buscarObrigatoria(pauta.getId())).thenReturn(sessao);
        when(votoRepository.contarPorOpcao(sessao.getId()))
                .thenReturn(List.of(contagem(OpcaoVoto.SIM, 4)));

        servicoEm(ABERTURA.plusSeconds(600)).apurar(pauta.getId());
        // Instancia nova, mesmo cache: simula outra requisicao no mesmo processo.
        var outra = servicoEm(ABERTURA.plusSeconds(700)).apurar(pauta.getId());

        verify(votoRepository, times(1)).contarPorOpcao(sessao.getId());
        assertThat(outra.votosSim()).isEqualTo(4);
    }

    @Test
    @DisplayName("propaga a falha quando a pauta nunca teve sessao")
    void semSessao() {
        var id = pauta.getId();
        when(sessaoService.buscarObrigatoria(id))
                .thenThrow(
                        new br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException(
                                id));

        var servico = servicoEm(ABERTURA);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servico.apurar(id))
                .isInstanceOf(
                        br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException.class);
        verify(votoRepository, never()).contarPorOpcao(org.mockito.ArgumentMatchers.any());
    }
}
