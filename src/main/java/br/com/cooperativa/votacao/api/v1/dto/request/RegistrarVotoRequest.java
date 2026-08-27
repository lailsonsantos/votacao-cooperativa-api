package br.com.cooperativa.votacao.api.v1.dto.request;

import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

@Schema(description = "Voto de um associado em uma pauta")
public record RegistrarVotoRequest(
        @Schema(
                        example = "19839091069",
                        description = "CPF do associado, com ou sem pontuação",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "O identificador do associado é obrigatório.")
                @CPF(message = "O CPF informado não é válido.")
                String associadoId,
        @Schema(example = "SIM", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "A opção de voto é obrigatória (SIM ou NAO).")
                OpcaoVoto opcao) {}
