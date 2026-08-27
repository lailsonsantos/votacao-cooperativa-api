package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import java.util.UUID;

/**
 * Caso de uso de apuracao do resultado de uma pauta.
 *
 * @see br.com.cooperativa.votacao.application.impl.ResultadoServiceImpl
 */
public interface ResultadoService {

    /**
     * Apura o resultado de uma pauta.
     *
     * <p>A consulta e permitida com a sessao aberta; nesse caso o resultado vem
     * marcado como parcial, porque o numero ainda pode mudar.
     *
     * @param pautaId identificador da pauta
     * @return o resultado apurado
     * @throws SessaoNaoAbertaException se a pauta nunca teve sessao
     */
    ResultadoVotacao apurar(UUID pautaId);
}
