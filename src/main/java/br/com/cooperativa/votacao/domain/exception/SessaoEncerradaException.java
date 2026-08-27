package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

public class SessaoEncerradaException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param pautaId identificador da pauta cuja sessao ja fechou
     */
    public SessaoEncerradaException(UUID pautaId) {
        super(
                TipoErro.REGRA_VIOLADA,
                "sessao-encerrada",
                "A sessao de votacao da pauta %s ja foi encerrada.".formatted(pautaId));
    }

    @Override
    public String getTitulo() {
        return "Sessao encerrada";
    }
}
