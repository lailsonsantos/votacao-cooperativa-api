package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;

public interface AssociadoValidator {

    /**
     * Garante que o associado pode votar, ou interrompe o fluxo.
     *
     * @param cpf CPF do associado, já validado
     * @throws AssociadoNaoAutorizadoException se o CPF for desconhecido ou estiver impedido
     */
    void validarPodeVotar(Cpf cpf);
}
