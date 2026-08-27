package br.com.cooperativa.votacao.domain.exception;

import lombok.Getter;

@Getter
public abstract class NegocioException extends RuntimeException {

    /** Natureza da falha, em vocabulario de dominio. */
    private final TipoErro tipo;

    /** Codigo estavel do erro, publicado na documentacao da API. */
    private final String codigo;

    /**
     * Cria a excecao de negocio.
     *
     * @param tipo natureza da falha
     * @param codigo identificador estavel do erro, em kebab-case
     * @param mensagem mensagem destinada a quem consome a aplicacao
     */
    protected NegocioException(TipoErro tipo, String codigo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    /**
     * Cria a excecao de negocio preservando a causa original.
     *
     * @param tipo natureza da falha
     * @param codigo identificador estavel do erro, em kebab-case
     * @param mensagem mensagem destinada a quem consome a aplicacao
     * @param causa excecao de origem
     */
    protected NegocioException(TipoErro tipo, String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    /**
     * Titulo curto do erro, apresentado a quem o recebe.
     *
     * @return o titulo legivel do erro
     */
    public abstract String getTitulo();
}
