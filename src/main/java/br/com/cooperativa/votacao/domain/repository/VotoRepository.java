package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.Voto;
import java.util.List;
import java.util.UUID;

public interface VotoRepository {

    /**
     * Persiste o voto e confirma a gravação imediatamente.
     *
     * @param voto voto a persistir
     * @return o voto persistido
     */
    Voto salvarEConfirmar(Voto voto);

    /**
     * Conta os votos de uma sessão agrupados por opção.
     *
     * @param sessaoId identificador da sessão
     * @return uma linha por opção efetivamente votada
     */
    List<ContagemVotos> contarPorOpcao(UUID sessaoId);

    /**
     * Indica se o associado já votou na sessão.
     *
     * @param sessaoId identificador da sessão
     * @param associadoId CPF do associado
     * @return {@code true} se já existir voto do associado na sessão
     */
    boolean existeVotoDoAssociado(UUID sessaoId, String associadoId);
}
