package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

public class RecursoNaoEncontradoException extends NegocioException {

    public RecursoNaoEncontradoException(String recurso, UUID id) {
        super(
                TipoErro.NAO_ENCONTRADO,
                "recurso-nao-encontrado",
                "%s não encontrada para o identificador %s.".formatted(recurso, id));
    }

    @Override
    public String getTitulo() {
        return "Recurso não encontrado";
    }
}
