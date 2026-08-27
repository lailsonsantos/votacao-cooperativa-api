package br.com.cooperativa.votacao.domain.exception;

public class AssociadoNaoAutorizadoException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param mensagem explicacao destinada ao associado
     */
    public AssociadoNaoAutorizadoException(String mensagem) {
        super(TipoErro.REGRA_VIOLADA, "associado-nao-autorizado", mensagem);
    }

    @Override
    public String getTitulo() {
        return "Associado nao autorizado a votar";
    }
}
