package br.com.cooperativa.votacao.infrastructure.integration.userinfo;

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
