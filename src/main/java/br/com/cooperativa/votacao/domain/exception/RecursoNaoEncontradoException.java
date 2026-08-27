package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.UUID;

/** Lancada quando a pauta referenciada na requisicao nao existe. */
public class RecursoNaoEncontradoException extends NegocioException {
    /**
     * Cria a excecao para uma pauta inexistente.
     *
     * @param recurso nome do recurso procurado, ex.: {@code Pauta}
     * @param id identificador informado na requisicao
     */
    public RecursoNaoEncontradoException(String recurso, UUID id) {
        super(
                TipoErro.NAO_ENCONTRADO,
                "recurso-nao-encontrado",
                "%s nao encontrada para o identificador %s.".formatted(recurso, id));
    }

    @Override
    public String getTitulo() {
        return "Recurso nao encontrado";
    }
}
