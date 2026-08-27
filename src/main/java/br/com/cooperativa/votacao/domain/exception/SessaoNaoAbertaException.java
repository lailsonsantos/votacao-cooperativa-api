package br.com.cooperativa.votacao.domain.exception;

import java.util.UUID;

/**
 * Lancada ao votar ou apurar uma pauta que nunca teve sessao aberta.
 *
 * <p>Devolve {@code 409} e nao {@code 404}: a pauta existe, o que falta e uma
 * pre-condicao de estado. Distinguir os dois casos evita que o cliente conclua
 * erroneamente que a pauta foi removida.
 */
public class SessaoNaoAbertaException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param pautaId identificador da pauta sem sessao
     */
    public SessaoNaoAbertaException(UUID pautaId) {
        super(
                TipoErro.CONFLITO,
                "sessao-nao-aberta",
                "A pauta %s ainda nao teve sessao de votacao aberta.".formatted(pautaId));
    }

    /** {@inheritDoc} */
    @Override
    public String getTitulo() {
        return "Sessao nao aberta";
    }
}
