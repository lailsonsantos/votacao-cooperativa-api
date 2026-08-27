package br.com.cooperativa.votacao.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Raiz das falhas previstas pelas regras de negocio.
 *
 * <p>Cada subclasse carrega o proprio {@link HttpStatus} e um {@code tipo}
 * estavel. O tratador global apenas traduz esses dados para a resposta, sem
 * conhecer nenhuma regra &mdash; assim uma regra nova nao exige alteracao no
 * tratador, e a decisao de qual status devolver fica junto da regra que a
 * motiva.
 *
 * <p>Falhas de negocio nao sao erros de sistema: um voto duplicado significa que
 * a aplicacao funcionou corretamente ao recusa-lo. Por isso sao registradas em
 * {@code WARN}, nunca em {@code ERROR}.
 */
public abstract class NegocioException extends RuntimeException {

    /** Status HTTP correspondente a esta falha. */
    private final HttpStatus status;

    /** Identificador estavel do tipo de erro, usado no campo {@code type} do ProblemDetail. */
    private final String tipo;

    /**
     * Cria a excecao de negocio.
     *
     * @param status   status HTTP a devolver
     * @param tipo     identificador estavel do erro, em kebab-case
     * @param mensagem mensagem destinada ao consumidor da API
     */
    protected NegocioException(HttpStatus status, String tipo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.tipo = tipo;
    }

    /**
     * Cria a excecao de negocio preservando a causa original.
     *
     * @param status   status HTTP a devolver
     * @param tipo     identificador estavel do erro, em kebab-case
     * @param mensagem mensagem destinada ao consumidor da API
     * @param causa    excecao de origem
     */
    protected NegocioException(HttpStatus status, String tipo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.status = status;
        this.tipo = tipo;
    }

    /**
     * Status HTTP a devolver ao cliente.
     *
     * @return o status associado a esta falha
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Identificador estavel do tipo de erro.
     *
     * @return o tipo em kebab-case, ex.: {@code voto-duplicado}
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Titulo curto do erro, apresentado no campo {@code title} do ProblemDetail.
     *
     * @return o titulo legivel do erro
     */
    public abstract String getTitulo();
}
