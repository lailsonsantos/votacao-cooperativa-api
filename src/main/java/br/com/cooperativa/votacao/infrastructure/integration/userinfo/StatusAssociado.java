package br.com.cooperativa.votacao.infrastructure.integration.userinfo;

/**
 * Situacao do associado devolvida pelo servico externo de CPF.
 *
 * <p>Contrato definido na Tarefa Bonus 1 do enunciado. Segundo o proprio enunciado, o servico
 * devolve resultados aleatorios: o mesmo CPF pode ser aceito em uma consulta e recusado na
 * seguinte.
 */
public enum StatusAssociado {
    /** O associado pode votar. */
    ABLE_TO_VOTE,

    /** O associado esta impedido de votar. */
    UNABLE_TO_VOTE
}
