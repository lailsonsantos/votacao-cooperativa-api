package br.com.cooperativa.votacao.domain.exception;

/**
 * Lancada quando o servico externo nega o direito de voto do associado.
 *
 * <p>Cobre os dois desfechos negativos da Tarefa Bonus 1: CPF desconhecido ({@code 404} no servico
 * externo) e CPF conhecido porem sem permissao ({@code UNABLE_TO_VOTE}).
 */
public class AssociadoNaoAutorizadoException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param mensagem explicacao destinada ao associado
     */
    public AssociadoNaoAutorizadoException(String mensagem) {
        super(TipoErro.REGRA_VIOLADA, "associado-nao-autorizado", mensagem);
    }

    /** {@inheritDoc} */
    @Override
    public String getTitulo() {
        return "Associado nao autorizado a votar";
    }
}
