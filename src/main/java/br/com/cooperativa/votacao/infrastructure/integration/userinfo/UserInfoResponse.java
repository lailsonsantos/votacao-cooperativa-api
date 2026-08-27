package br.com.cooperativa.votacao.infrastructure.integration.userinfo;

/**
 * Corpo da resposta de {@code GET /users/{cpf}} do servico externo.
 *
 * @param status situacao do associado quanto ao direito de voto
 */
public record UserInfoResponse(StatusAssociado status) {
    /**
     * Indica se o associado esta habilitado a votar.
     *
     * @return {@code true} quando o status for {@link StatusAssociado#ABLE_TO_VOTE}
     */
    public boolean podeVotar() {
        return status == StatusAssociado.ABLE_TO_VOTE;
    }
}
