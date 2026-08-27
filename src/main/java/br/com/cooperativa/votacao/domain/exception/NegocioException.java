package br.com.cooperativa.votacao.domain.exception;

import lombok.Getter;

/**
 * Raiz das falhas previstas pelas regras de negocio.
 *
 * <p>Cada subclasse declara a <em>natureza</em> da falha ({@link TipoErro}) e um
 * codigo estavel, ambos em vocabulario de dominio. Nenhuma delas conhece HTTP:
 * a traducao para status acontece na camada de API, que e onde HTTP significa
 * alguma coisa.
 *
 * <p>O tratador global apenas transporta esses dados, sem conhecer regra alguma.
 * Assim, uma regra nova nao exige alteracao no tratador, e a decisao sobre a
 * natureza do erro fica junto da regra que a motiva.
 *
 * <p>Falhas de negocio nao sao erros de sistema: um voto duplicado significa que
 * a aplicacao funcionou corretamente ao recusa-lo. Por isso sao registradas em
 * {@code WARN}, nunca em {@code ERROR}.
 */
@Getter
public abstract class NegocioException extends RuntimeException {

    /** Natureza da falha, em vocabulario de dominio. */
    private final TipoErro tipo;

    /** Codigo estavel do erro, publicado na documentacao da API. */
    private final String codigo;

    /**
     * Cria a excecao de negocio.
     *
     * @param tipo     natureza da falha
     * @param codigo   identificador estavel do erro, em kebab-case
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
     * @param tipo     natureza da falha
     * @param codigo   identificador estavel do erro, em kebab-case
     * @param mensagem mensagem destinada a quem consome a aplicacao
     * @param causa    excecao de origem
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
