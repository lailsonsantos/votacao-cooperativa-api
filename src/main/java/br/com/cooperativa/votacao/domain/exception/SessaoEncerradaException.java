package br.com.cooperativa.votacao.domain.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Lancada ao tentar votar depois do prazo da sessao.
 *
 * <p>Devolve {@code 422}: a requisicao esta sintaticamente correta, mas nao pode
 * ser processada porque a janela de votacao ja expirou.
 */
public class SessaoEncerradaException extends NegocioException {

    /**
     * Cria a excecao.
     *
     * @param pautaId identificador da pauta cuja sessao ja fechou
     */
    public SessaoEncerradaException(UUID pautaId) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "sessao-encerrada",
                "A sessao de votacao da pauta %s ja foi encerrada.".formatted(pautaId));
    }

    /** {@inheritDoc} */
    @Override
    public String getTitulo() {
        return "Sessao encerrada";
    }
}
