package br.com.cooperativa.votacao.domain.model;

import br.com.cooperativa.votacao.domain.enums.ResultadoApuracao;
import br.com.cooperativa.votacao.domain.enums.StatusSessao;
import java.util.UUID;

public record ResultadoVotacao(
        UUID pautaId,
        String titulo,
        StatusSessao status,
        long votosSim,
        long votosNao,
        ResultadoApuracao resultado) {

    public static ResultadoVotacao de(
            UUID pautaId, String titulo, StatusSessao status, long votosSim, long votosNao) {
        return new ResultadoVotacao(
                pautaId, titulo, status, votosSim, votosNao, apurar(votosSim, votosNao));
    }

    public long totalVotos() {
        return votosSim + votosNao;
    }

    public boolean parcial() {
        return status == StatusSessao.ABERTA;
    }

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
