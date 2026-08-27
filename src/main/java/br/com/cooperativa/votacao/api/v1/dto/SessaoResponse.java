package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.model.StatusSessao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

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
     * @param sessao entidade de origem
     * @param agora instante de referencia, vindo do relogio injetado
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
