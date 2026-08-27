package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;

public interface ContagemVotos {
    /**
     * Opção votada.
     *
     * @return a opção a que este total se refere
     */
    OpcaoVoto getOpcao();

    /**
     * Quantidade de votos para a opção.
     *
     * @return o total de votos
     */
    long getTotal();
}
