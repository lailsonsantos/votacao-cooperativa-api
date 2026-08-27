package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida para a persistencia de sessoes de votacao.
 */
public interface SessaoVotacaoRepository {

    /**
     * Persiste a sessao e confirma a gravacao imediatamente.
     *
     * <p>A confirmacao imediata e necessaria para que a violacao da constraint
     * {@code uk_sessao_pauta} ocorra dentro do metodo que a provoca, e possa ser
     * traduzida em erro de negocio. Sem isso, a falha so apareceria no commit da
     * transacao, longe do codigo capaz de interpreta-la.
     *
     * @param sessao sessao a persistir
     * @return a sessao persistida
     */
    SessaoVotacao salvarEConfirmar(SessaoVotacao sessao);

    /**
     * Busca a sessao de uma pauta, junto com a pauta.
     *
     * @param pautaId identificador da pauta
     * @return a sessao, se a pauta ja tiver tido uma aberta
     */
    Optional<SessaoVotacao> buscarPorPauta(UUID pautaId);

    /**
     * Indica se a pauta ja possui sessao.
     *
     * @param pautaId identificador da pauta
     * @return {@code true} se ja existir uma sessao para a pauta
     */
    boolean existePorPauta(UUID pautaId);
}
