package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.OpcaoVoto;

public interface ContagemVotos {
    /**
     * Opcao votada.
     *
     * @return a opcao a que este total se refere
     */
    OpcaoVoto getOpcao();

    /**
     * Quantidade de votos para a opcao.
     *
     * @return o total de votos
     */
    long getTotal();
}
