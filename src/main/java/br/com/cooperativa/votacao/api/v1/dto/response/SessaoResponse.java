package br.com.cooperativa.votacao.api.v1.dto.response;

import br.com.cooperativa.votacao.domain.enums.StatusSessao;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Sessão de votação de uma pauta")
public record SessaoResponse(
        UUID id,
        UUID pautaId,
        Instant aberturaEm,
        Instant fechamentoEm,
        StatusSessao status,
        long segundosRestantes) {
    /**
     * Converte a entidade para a representação da API.
     *
     * @param sessao entidade de origem
     * @param agora instante de referência, vindo do relógio injetado
     * @return a representação correspondente
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
