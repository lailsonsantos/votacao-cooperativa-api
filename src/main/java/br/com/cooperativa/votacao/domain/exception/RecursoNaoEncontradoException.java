package br.com.cooperativa.votacao.domain.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Lancada quando a pauta referenciada na requisicao nao existe.
 */
public class RecursoNaoEncontradoException extends NegocioException {
    /**
     * Cria a excecao para uma pauta inexistente.
     *
     * @param recurso nome do recurso procurado, ex.: {@code Pauta}
     * @param id      identificador informado na requisicao
     */
    public RecursoNaoEncontradoException(String recurso, UUID id) {
        super(
                HttpStatus.NOT_FOUND,
                "recurso-nao-encontrado",
                "%s nao encontrada para o identificador %s.".formatted(recurso, id));
    }

    /** {@inheritDoc} */
    @Override
    public String getTitulo() {
        return "Recurso nao encontrado";
    }
}
