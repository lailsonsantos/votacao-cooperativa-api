package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import java.util.UUID;

public interface ResultadoService {

    /**
     * Apura o resultado de uma pauta.
     *
     * @param pautaId identificador da pauta
     * @return o resultado apurado
     * @throws SessaoNaoAbertaException se a pauta nunca teve sessao
     */
    ResultadoVotacao apurar(UUID pautaId);
}
