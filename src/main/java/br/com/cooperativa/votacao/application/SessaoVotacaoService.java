package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.exception.SessaoJaAbertaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.util.Optional;
import java.util.UUID;

public interface SessaoVotacaoService {

    /**
     * Abre a sessão de votação de uma pauta.
     *
     * @param pautaId identificador da pauta
     * @param duracaoMinutos duração solicitada, ou {@code null} para usar o padrão
     * @return a sessão aberta
     * @throws RecursoNaoEncontradoException se a pauta não existir
     * @throws SessaoJaAbertaException se a pauta já possuir uma sessão
     */
    SessaoVotacao abrir(UUID pautaId, Integer duracaoMinutos);

    /**
     * Busca a sessão de uma pauta, exigindo que ela exista.
     *
     * @param pautaId identificador da pauta
     * @return a sessão encontrada
     * @throws SessaoNaoAbertaException se a pauta nunca teve sessão aberta
     */
    SessaoVotacao buscarObrigatoria(UUID pautaId);

    /**
     * Busca a sessão de uma pauta, se existir.
     *
     * @param pautaId identificador da pauta
     * @return a sessão, se houver
     */
    Optional<SessaoVotacao> buscar(UUID pautaId);
}
