package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.exception.SessaoJaAbertaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de abertura e consulta de sessoes de votacao.
 *
 * @see br.com.cooperativa.votacao.application.impl.SessaoVotacaoServiceImpl
 */
public interface SessaoVotacaoService {

    /**
     * Abre a sessao de votacao de uma pauta.
     *
     * <p>Quando a chamada nao informa duracao, aplica-se o padrao de 1 minuto definido no
     * enunciado. O valor vem de configuracao para que nao exista numero magico no codigo e para
     * permitir ajuste por ambiente.
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
     * <p>Variante tolerante usada pelas telas, que precisam desenhar tanto o caso "sessao aberta"
     * quanto o caso "ainda sem sessao" sem tratar excecao.
     *
     * @param pautaId identificador da pauta
     * @return a sessao, se houver
     */
    Optional<SessaoVotacao> buscar(UUID pautaId);
}
