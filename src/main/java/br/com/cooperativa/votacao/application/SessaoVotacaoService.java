package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.exception.SessaoJaAbertaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.util.Optional;
import java.util.UUID;

public interface SessaoVotacaoService {

    /**
     * Abre a sessao de votacao de uma pauta.
     *
     * @param pautaId identificador da pauta
     * @param duracaoMinutos duracao solicitada, ou {@code null} para usar o padrao
     * @return a sessao aberta
     * @throws RecursoNaoEncontradoException se a pauta nao existir
     * @throws SessaoJaAbertaException se a pauta ja possuir uma sessao
     */
    SessaoVotacao abrir(UUID pautaId, Integer duracaoMinutos);

    /**
     * Busca a sessao de uma pauta, exigindo que ela exista.
     *
     * @param pautaId identificador da pauta
     * @return a sessao encontrada
     * @throws SessaoNaoAbertaException se a pauta nunca teve sessao aberta
     */
    SessaoVotacao buscarObrigatoria(UUID pautaId);

    /**
     * Busca a sessao de uma pauta, se existir.
     *
     * @param pautaId identificador da pauta
     * @return a sessao, se houver
     */
    Optional<SessaoVotacao> buscar(UUID pautaId);
}
