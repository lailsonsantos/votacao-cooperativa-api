package br.com.cooperativa.votacao.domain.model;

import java.util.UUID;

/**
 * Apuracao de uma pauta em um dado instante.
 *
 * <p>Nao e uma entidade e nao e persistida: o resultado e sempre derivado da
 * contagem agregada dos votos. Persistir uma contagem criaria um segundo lugar
 * onde a verdade poderia divergir do fato.
 *
 * @param pautaId    identificador da pauta apurada
 * @param titulo     titulo da pauta, para exibicao direta na tela
 * @param status     situacao da sessao no momento da apuracao
 * @param votosSim   quantidade de votos favoraveis
 * @param votosNao   quantidade de votos contrarios
 * @param resultado  desfecho calculado a partir da contagem
 */
public record ResultadoVotacao(
        UUID pautaId,
        String titulo,
        StatusSessao status,
        long votosSim,
        long votosNao,
        ResultadoApuracao resultado) {

    /**
     * Monta o resultado a partir da contagem bruta.
     *
     * @param pautaId  identificador da pauta
     * @param titulo   titulo da pauta
     * @param status   situacao da sessao
     * @param votosSim quantidade de votos favoraveis
     * @param votosNao quantidade de votos contrarios
     * @return o resultado com o desfecho ja calculado
     */
    public static ResultadoVotacao de(
            UUID pautaId, String titulo, StatusSessao status, long votosSim, long votosNao) {
        return new ResultadoVotacao(
                pautaId, titulo, status, votosSim, votosNao, apurar(votosSim, votosNao));
    }

    /**
     * Total de votos registrados.
     *
     * @return a soma dos votos favoraveis e contrarios
     */
    public long totalVotos() {
        return votosSim + votosNao;
    }

    /**
     * Indica se a apuracao ainda e parcial.
     *
     * <p>Consultar o resultado com a sessao aberta e permitido, mas o consumidor
     * precisa saber que o numero ainda pode mudar &mdash; por isso o status
     * acompanha a resposta.
     *
     * @return {@code true} enquanto a sessao estiver aberta
     */
    public boolean parcial() {
        return status == StatusSessao.ABERTA;
    }

    /**
     * Determina o desfecho a partir da contagem.
     *
     * @param votosSim quantidade de votos favoraveis
     * @param votosNao quantidade de votos contrarios
     * @return o desfecho correspondente
     */
    private static ResultadoApuracao apurar(long votosSim, long votosNao) {
        if (votosSim == 0 && votosNao == 0) {
            return ResultadoApuracao.SEM_VOTOS;
        }
        if (votosSim > votosNao) {
            return ResultadoApuracao.APROVADA;
        }
        return votosSim < votosNao ? ResultadoApuracao.REPROVADA : ResultadoApuracao.EMPATE;
    }
}
