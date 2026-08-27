package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.Pauta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Pauta cadastrada")
public record PautaResponse(UUID id, String titulo, String descricao, Instant criadaEm) {
    /**
     * Converte a entidade de dominio para a representacao da API.
     *
     * @param pauta entidade de origem
     * @return a representacao correspondente
     */
    public static PautaResponse de(Pauta pauta) {
        return new PautaResponse(
                pauta.getId(), pauta.getTitulo(), pauta.getDescricao(), pauta.getCriadaEm());
    }
}
