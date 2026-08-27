package br.com.cooperativa.votacao.domain.exception;

import java.util.UUID;

public class SessaoJaAbertaException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param pautaId identificador da pauta que ja possui sessao
     */
    public SessaoJaAbertaException(UUID pautaId) {
        super(
                TipoErro.CONFLITO,
                "sessao-ja-aberta",
                "A pauta %s ja possui uma sessao de votacao.".formatted(pautaId));
    }

    @Override
    public String getTitulo() {
        return "Sessao ja aberta";
    }
}
