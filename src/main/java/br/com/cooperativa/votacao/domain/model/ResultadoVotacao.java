package br.com.cooperativa.votacao.domain.model;

import java.util.UUID;

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
     * @param pautaId identificador da pauta
     * @param titulo titulo da pauta
     * @param status situacao da sessao
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
