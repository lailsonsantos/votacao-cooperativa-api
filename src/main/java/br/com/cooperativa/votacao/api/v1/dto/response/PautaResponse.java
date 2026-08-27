package br.com.cooperativa.votacao.api.v1.dto.response;

import br.com.cooperativa.votacao.domain.model.Pauta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Pauta cadastrada")
public record PautaResponse(UUID id, String titulo, String descricao, Instant criadaEm) {
    /**
     * Converte a entidade de domínio para a representação da API.
     *
     * @param pauta entidade de origem
     * @return a representação correspondente
     */
    public static PautaResponse de(Pauta pauta) {
        return new PautaResponse(
                pauta.getId(), pauta.getTitulo(), pauta.getDescricao(), pauta.getCriadaEm());
    }
}
