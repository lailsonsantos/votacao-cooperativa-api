package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

@Schema(description = "Voto de um associado em uma pauta")
public record RegistrarVotoRequest(
        @Schema(
                        example = "19839091069",
                        description = "CPF do associado, com ou sem pontuacao",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "O identificador do associado e obrigatorio.")
                @CPF(message = "O CPF informado nao e valido.")
                String associadoId,
        @Schema(example = "SIM", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "A opcao de voto e obrigatoria (SIM ou NAO).")
                OpcaoVoto opcao) {}
