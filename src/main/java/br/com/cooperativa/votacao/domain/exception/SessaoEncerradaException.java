package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

public class SessaoEncerradaException extends NegocioException {

    public SessaoEncerradaException(UUID pautaId) {
        super(
                TipoErro.REGRA_VIOLADA,
                "sessao-encerrada",
                "A sessão de votação da pauta %s já foi encerrada.".formatted(pautaId));
    }

    @Override
    public String getTitulo() {
        return "Sessão encerrada";
    }
}
