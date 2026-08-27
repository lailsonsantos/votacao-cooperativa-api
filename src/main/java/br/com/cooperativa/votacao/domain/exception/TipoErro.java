package br.com.cooperativa.votacao.domain.exception;

/**
 * Natureza de uma falha de negocio, expressa em vocabulario de dominio.
 *
 * <p>Substitui o {@code HttpStatus} que as excecoes carregavam. HTTP e um
 * detalhe de transporte: a mesma regra de negocio, exposta por mensageria ou por
 * gRPC, continuaria valendo e nao teria status algum. Um dominio que conhece
 * codigos HTTP e um dominio acoplado a uma forma especifica de ser consumido.
 *
 * <p>A traducao para status acontece na camada de API, unico lugar onde HTTP e
 * a linguagem correta &mdash; ver {@code MapeadorDeStatus}.
 *
 * <p>O conjunto e deliberadamente pequeno e semantico. Uma regra nova escolhe
 * uma das naturezas existentes e o tratador de erros nao muda, o que preserva o
 * principio aberto-fechado: estende-se o dominio sem alterar quem o traduz.
 */
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
