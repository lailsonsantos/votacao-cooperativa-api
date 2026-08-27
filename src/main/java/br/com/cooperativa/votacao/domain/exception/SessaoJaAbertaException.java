package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

public class SessaoJaAbertaException extends NegocioException {

    public SessaoJaAbertaException(UUID pautaId) {
        super(
                TipoErro.CONFLITO,
                "sessao-ja-aberta",
                "A pauta %s já possui uma sessão de votação.".formatted(pautaId));
    }

    @Override
    public String getTitulo() {
        return "Sessão já aberta";
    }
}
