package br.com.cooperativa.votacao.api.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de uma nova pauta")
public record CriarPautaRequest(
        @Schema(example = "Reforma do estatuto social", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "O título da pauta é obrigatório.")
                @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
                String titulo,
        @Schema(example = "Atualizacao dos artigos 12 a 18 do estatuto social.")
                @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
                String descricao) {}
