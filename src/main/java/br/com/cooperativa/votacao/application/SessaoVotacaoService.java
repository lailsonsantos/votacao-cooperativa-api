package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.config.AppProperties;
import br.com.cooperativa.votacao.domain.exception.SessaoJaAbertaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.repository.SessaoVotacaoRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de abertura e consulta de sessoes de votacao.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessaoVotacaoService {
    private final SessaoVotacaoRepository sessaoRepository;
    private final PautaService pautaService;
    private final AppProperties appProperties;
    private final Clock clock;

    /**
     * Abre a sessao de votacao de uma pauta.
     *
     * <p>Quando a chamada nao informa duracao, aplica-se o padrao de 1 minuto
     * definido no enunciado. O valor vem de configuracao para que nao exista
     * numero magico no codigo e para permitir ajuste por ambiente.
     *
     * @param pautaId        identificador da pauta
     * @param duracaoMinutos duracao solicitada, ou {@code null} para usar o padrao
     * @return a sessao aberta
     * @throws br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException
     *         se a pauta nao existir
     * @throws SessaoJaAbertaException se a pauta ja possuir uma sessao
     */
    @Transactional
    public SessaoVotacao abrir(UUID pautaId, Integer duracaoMinutos) {
        var pauta = pautaService.buscar(pautaId);

        // Checagem antecipada apenas para devolver a mensagem de negocio correta.
        // A garantia real e a constraint uk_sessao_pauta, tratada no catch abaixo,
        // que resolve a corrida entre duas aberturas simultaneas.
        if (sessaoRepository.existsByPautaId(pautaId)) {
            throw new SessaoJaAbertaException(pautaId);
        }

        var minutos =
                duracaoMinutos != null
                        ? duracaoMinutos
                        : appProperties.sessao().duracaoPadraoMinutos();

        var sessao =
                SessaoVotacao.abrir(pauta, clock.instant(), Duration.ofMinutes(minutos));

        try {
            var salva = sessaoRepository.saveAndFlush(sessao);
            log.info(
                    "Sessao de votacao aberta. pautaId={} sessaoId={} duracaoMinutos={} "
                            + "fechamentoEm={}",
                    pautaId,
                    salva.getId(),
                    minutos,
                    salva.getFechamentoEm());
            return salva;
        } catch (DataIntegrityViolationException e) {
            throw new SessaoJaAbertaException(pautaId);
        }
    }

    /**
     * Busca a sessao de uma pauta, exigindo que ela exista.
     *
     * @param pautaId identificador da pauta
     * @return a sessao encontrada
     * @throws SessaoNaoAbertaException se a pauta nunca teve sessao aberta
     */
    @Transactional(readOnly = true)
    public SessaoVotacao buscarObrigatoria(UUID pautaId) {
        return buscar(pautaId).orElseThrow(() -> new SessaoNaoAbertaException(pautaId));
    }

    /**
     * Busca a sessao de uma pauta, se existir.
     *
     * <p>Variante tolerante usada pelas telas, que precisam desenhar tanto o caso
     * "sessao aberta" quanto o caso "ainda sem sessao" sem tratar excecao.
     *
     * @param pautaId identificador da pauta
     * @return a sessao, se houver
     */
    @Transactional(readOnly = true)
    public Optional<SessaoVotacao> buscar(UUID pautaId) {
        return sessaoRepository.findByPautaId(pautaId);
    }
}
