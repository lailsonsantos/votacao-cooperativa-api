package br.com.cooperativa.votacao.application.impl;

import br.com.cooperativa.votacao.application.ResultadoService;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import br.com.cooperativa.votacao.config.CacheConfig;
import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import br.com.cooperativa.votacao.domain.enums.StatusSessao;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.repository.ContagemVotos;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultadoServiceImpl implements ResultadoService {

    private final VotoRepository votoRepository;
    private final SessaoVotacaoService sessaoService;
    private final CacheManager cacheManager;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ResultadoVotacao apurar(UUID pautaId) {
        var sessao = sessaoService.buscarObrigatoria(pautaId);
        var status = sessao.status(clock.instant());

        if (status == StatusSessao.FECHADA) {
            var cache = cacheManager.getCache(CacheConfig.CACHE_RESULTADO);
            var cacheado = cache != null ? cache.get(pautaId, ResultadoVotacao.class) : null;
            if (cacheado != null) {
                return cacheado;
            }
            var resultado = contar(sessao, status);
            guardar(cache, pautaId, resultado);
            return resultado;
        }

        return contar(sessao, status);
    }

    /**
     * Executa a contagem agregada e monta o resultado.
     *
     * @param sessao sessao a apurar
     * @param status situacao da sessao no momento da apuracao
     * @return o resultado com o desfecho calculado
     */
    private ResultadoVotacao contar(SessaoVotacao sessao, StatusSessao status) {
        Map<OpcaoVoto, Long> totais =
                votoRepository.contarPorOpcao(sessao.getId()).stream()
                        .collect(
                                Collectors.toMap(
                                        ContagemVotos::getOpcao,
                                        ContagemVotos::getTotal,
                                        Long::sum,
                                        () -> new EnumMap<>(OpcaoVoto.class)));

        var pauta = sessao.getPauta();
        var resultado =
                ResultadoVotacao.de(
                        pauta.getId(),
                        pauta.getTitulo(),
                        status,
                        totais.getOrDefault(OpcaoVoto.SIM, 0L),
                        totais.getOrDefault(OpcaoVoto.NAO, 0L));

        log.info(
                "Resultado apurado. pautaId={} status={} sim={} nao={} desfecho={}",
                resultado.pautaId(),
                resultado.status(),
                resultado.votosSim(),
                resultado.votosNao(),
                resultado.resultado());

        return resultado;
    }

    /**
     * Guarda o resultado no cache, se o cache estiver disponivel.
     *
     * @param cache cache de resultados, possivelmente nulo
     * @param pautaId chave de cache
     * @param resultado valor a guardar
     */
    private void guardar(Cache cache, UUID pautaId, ResultadoVotacao resultado) {
        if (cache != null) {
            cache.put(pautaId, resultado);
        }
    }
}
