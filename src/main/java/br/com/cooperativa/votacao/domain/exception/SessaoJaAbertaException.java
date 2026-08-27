package br.com.cooperativa.votacao.domain.exception;

import java.util.UUID;

/**
 * Lancada ao tentar abrir uma segunda sessao para a mesma pauta.
 *
 * <p>O enunciado descreve "abrir uma sessao de votacao em uma pauta", no singular. Permitir uma
 * segunda rodada mudaria a semantica do resultado apurado, entao a operacao e recusada. A premissa
 * esta registrada no README entre as duvidas levantadas.
 */
public class SessaoJaAbertaException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param pautaId identificador da pauta que ja possui sessao
     */
    public SessaoJaAbertaException(UUID pautaId) {
        super(
                TipoErro.CONFLITO,
                "sessao-ja-aberta",
                "A pauta %s ja possui uma sessao de votacao.".formatted(pautaId));
    }

    /** {@inheritDoc} */
    @Override
    public String getTitulo() {
        return "Sessao ja aberta";
    }
}
