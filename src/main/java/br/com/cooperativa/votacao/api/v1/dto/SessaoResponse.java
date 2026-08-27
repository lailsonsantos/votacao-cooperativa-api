package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.model.StatusSessao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Representacao de uma sessao de votacao na API.
 *
 * @param id                     identificador da sessao
 * @param pautaId                identificador da pauta em deliberacao
 * @param aberturaEm             instante de abertura, em UTC
 * @param fechamentoEm           instante de encerramento, em UTC
 * @param status                 situacao derivada do relogio no momento da consulta
 * @param segundosRestantes      tempo restante de votacao, zero apos o fechamento
 */
@Schema(description = "Sessao de votacao de uma pauta")
public record SessaoResponse(
        UUID id,
        UUID pautaId,
        Instant aberturaEm,
        Instant fechamentoEm,
        StatusSessao status,
        long segundosRestantes) {

    /**
     * Converte a entidade para a representacao da API.
     *
     * <p>{@code status} e {@code segundosRestantes} sao calculados no momento da
     * resposta, e nao lidos do banco, porque ambos dependem do relogio.
     *
     * @param sessao entidade de origem
     * @param agora  instante de referencia, vindo do relogio injetado
     * @return a representacao correspondente
     */
    public static SessaoResponse de(SessaoVotacao sessao, Instant agora) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getPauta().getId(),
                sessao.getAberturaEm(),
                sessao.getFechamentoEm(),
                sessao.status(agora),
                sessao.tempoRestante(agora).toSeconds());
    }
}
