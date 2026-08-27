package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Corpo da requisicao de registro de voto.
 *
 * @param associadoId CPF do associado, com ou sem pontuacao
 * @param opcao       opcao escolhida
 */
@Schema(description = "Voto de um associado em uma pauta")
public record RegistrarVotoRequest(
        @Schema(
                        example = "19839091069",
                        description = "CPF do associado, com ou sem pontuacao",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "O identificador do associado e obrigatorio.")
                @Pattern(
                        regexp = "\\D*(\\d\\D*){11}",
                        message = "O CPF deve conter 11 digitos.")
                String associadoId,
        @Schema(example = "SIM", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "A opcao de voto e obrigatoria (SIM ou NAO).")
                OpcaoVoto opcao) {}
