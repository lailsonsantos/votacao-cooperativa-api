package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.Pauta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Representacao de uma pauta na API.
 *
 * @param id        identificador da pauta
 * @param titulo    titulo da pauta
 * @param descricao descricao do que esta em deliberacao
 * @param criadaEm  momento do cadastro, em UTC
 */
@Schema(description = "Pauta cadastrada")
public record PautaResponse(UUID id, String titulo, String descricao, Instant criadaEm) {
    /**
     * Converte a entidade de dominio para a representacao da API.
     *
     * <p>A conversao e explicita e manual. Uma biblioteca de mapeamento
     * resolveria o mesmo problema, mas adicionaria uma dependencia e uma etapa de
     * geracao de codigo para tres DTOs &mdash; complexidade sem contrapartida.
     *
     * @param pauta entidade de origem
     * @return a representacao correspondente
     */
    public static PautaResponse de(Pauta pauta) {
        return new PautaResponse(
                pauta.getId(), pauta.getTitulo(), pauta.getDescricao(), pauta.getCriadaEm());
    }
}
