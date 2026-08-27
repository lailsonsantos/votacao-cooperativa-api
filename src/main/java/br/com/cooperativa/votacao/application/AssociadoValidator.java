package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;

/**
 * Verificacao do direito de voto de um associado (Tarefa Bonus 1).
 *
 * <p>Isolar a decisao atras desta abstracao mantem o registro de voto indiferente a <em>como</em> a
 * aptidao e apurada. Hoje a resposta vem de um servico externo; se a cooperativa passar a manter
 * cadastro proprio, apenas a implementacao muda.
 *
 * @see br.com.cooperativa.votacao.application.impl.AssociadoValidatorImpl
 */
public interface AssociadoValidator {

    /**
     * Garante que o associado pode votar, ou interrompe o fluxo.
     *
     * @param cpf CPF do associado, ja validado
     * @throws AssociadoNaoAutorizadoException se o CPF for desconhecido ou estiver impedido
     */
    void validarPodeVotar(Cpf cpf);
}
