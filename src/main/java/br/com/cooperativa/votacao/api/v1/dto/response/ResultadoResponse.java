package br.com.cooperativa.votacao.api.v1.dto.response;

import br.com.cooperativa.votacao.domain.enums.ResultadoApuracao;
import br.com.cooperativa.votacao.domain.enums.StatusSessao;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Apuracao dos votos de uma pauta")
public record ResultadoResponse(
        UUID pautaId,
        String titulo,
        StatusSessao status,
        boolean parcial,
        long totalVotos,
        long votosSim,
        long votosNao,
        ResultadoApuracao resultado) {
    /**
     * Converte o resultado de domínio para a representação da API.
     *
     * @param resultado resultado de domínio
     * @return a representação correspondente
     */
    public static ResultadoResponse de(ResultadoVotacao resultado) {
        return new ResultadoResponse(
                resultado.pautaId(),
                resultado.titulo(),
                resultado.status(),
                resultado.parcial(),
                resultado.totalVotos(),
                resultado.votosSim(),
                resultado.votosNao(),
                resultado.resultado());
    }
}
