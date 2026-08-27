package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.Voto;
import java.util.List;
import java.util.UUID;

/** Porta de saida para a persistencia de votos. */
public interface VotoRepository {

    /**
     * Persiste o voto e confirma a gravacao imediatamente.
     *
     * @param voto voto a persistir
     * @return o voto persistido
     */
    Voto salvarEConfirmar(Voto voto);

    /**
     * Conta os votos de uma sessao agrupados por opcao.
     *
     * @param sessaoId identificador da sessao
     * @return uma linha por opcao efetivamente votada
     */
    List<ContagemVotos> contarPorOpcao(UUID sessaoId);

    /**
     * Indica se o associado ja votou na sessao.
     *
     * @param sessaoId identificador da sessao
     * @param associadoId CPF do associado
     * @return {@code true} se ja existir voto do associado na sessao
     */
    boolean existeVotoDoAssociado(UUID sessaoId, String associadoId);
}
