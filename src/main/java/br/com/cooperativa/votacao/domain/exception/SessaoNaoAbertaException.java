package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

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

    @Override
    public String getTitulo() {
        return "Sessao nao aberta";
    }
}
