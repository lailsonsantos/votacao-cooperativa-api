package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

public class SessaoJaAbertaException extends NegocioException {

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
