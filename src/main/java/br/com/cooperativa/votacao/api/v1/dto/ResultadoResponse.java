package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.ResultadoApuracao;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import br.com.cooperativa.votacao.domain.model.StatusSessao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Resultado da apuracao de uma pauta.
 *
 * @param pautaId identificador da pauta
 * @param titulo titulo da pauta
 * @param status situacao da sessao no momento da apuracao
 * @param parcial {@code true} enquanto a sessao estiver aberta
 * @param totalVotos soma dos votos registrados
 * @param votosSim quantidade de votos favoraveis
 * @param votosNao quantidade de votos contrarios
 * @param resultado desfecho calculado
 */
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
     * Converte o resultado de dominio para a representacao da API.
     *
     * <p>O campo {@code parcial} e explicito, e nao deixado para o consumidor inferir a partir de
     * {@code status}: consultar o resultado com a sessao aberta e permitido, e o cliente precisa
     * saber sem ambiguidade que o numero ainda pode mudar.
     *
     * @param resultado resultado de dominio
     * @return a representacao correspondente
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
