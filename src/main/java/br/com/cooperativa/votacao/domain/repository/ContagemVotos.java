package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.OpcaoVoto;

/**
 * Linha do resultado da contagem agregada de votos.
 *
 * <p>Projecao de interface do Spring Data: a consulta devolve uma linha por
 * opcao, e nenhuma entidade {@code Voto} e materializada. Com centenas de
 * milhares de votos (Tarefa Bonus 2), carregar as entidades para contar em
 * memoria esgotaria a heap; a contagem agregada e resolvida pelo indice.
 */
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
