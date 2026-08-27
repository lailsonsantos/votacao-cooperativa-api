package br.com.cooperativa.votacao.domain.exception;

public enum TipoErro {

    /** A entrada nao satisfaz o formato exigido. */
    ENTRADA_INVALIDA,

    /** O recurso referenciado nao existe. */
    NAO_ENCONTRADO,

    /** A operacao conflita com o estado atual do recurso. */
    CONFLITO,

    /** A requisicao e valida, mas uma regra de negocio impede o processamento. */
    REGRA_VIOLADA
}
