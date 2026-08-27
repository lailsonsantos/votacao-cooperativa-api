package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

public class SessaoNaoAbertaException extends NegocioException {

    public SessaoNaoAbertaException(UUID pautaId) {
        super(
                TipoErro.CONFLITO,
                "sessao-nao-aberta",
                "A pauta %s ainda não teve sessão de votação aberta.".formatted(pautaId));
    }

    @Override
    public String getTitulo() {
        return "Sessão não aberta";
    }
}
