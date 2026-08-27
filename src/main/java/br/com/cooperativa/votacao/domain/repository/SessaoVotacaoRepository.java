package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.util.Optional;
import java.util.UUID;

public interface SessaoVotacaoRepository {

    /**
     * Persiste a sessão e confirma a gravação imediatamente.
     *
     * @param sessão sessão a persistir
     * @return a sessão persistida
     */
    SessaoVotacao salvarEConfirmar(SessaoVotacao sessao);

    /**
     * Busca a sessão de uma pauta, junto com a pauta.
     *
     * @param pautaId identificador da pauta
     * @return a sessão, se a pauta já tiver tido uma aberta
     */
    Optional<SessaoVotacao> buscarPorPauta(UUID pautaId);

    /**
     * Indica se a pauta já possui sessão.
     *
     * @param pautaId identificador da pauta
     * @return {@code true} se já existir uma sessão para a pauta
     */
    boolean existePorPauta(UUID pautaId);
}
