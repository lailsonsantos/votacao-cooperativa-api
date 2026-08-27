package br.com.cooperativa.votacao.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisicao de cadastro de pauta.
 *
 * <p>A validacao vive na borda, com Bean Validation, e nao no dominio: entrada malformada e um
 * problema de protocolo, e deve ser recusada com {@code 400} antes de qualquer regra de negocio ser
 * avaliada.
 *
 * @param titulo titulo da pauta, obrigatorio
 * @param descricao descricao opcional do que esta em deliberacao
 */
@Schema(description = "Dados para cadastro de uma nova pauta")
public record CriarPautaRequest(
        @Schema(example = "Reforma do estatuto social", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "O titulo da pauta e obrigatorio.")
                @Size(max = 200, message = "O titulo deve ter no maximo 200 caracteres.")
                String titulo,
        @Schema(example = "Atualizacao dos artigos 12 a 18 do estatuto social.")
                @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres.")
                String descricao) {}
