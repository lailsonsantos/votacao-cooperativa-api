package br.com.cooperativa.votacao.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Schema(description = "Parametros de abertura da sessao de votacao")
public record AbrirSessaoRequest(
        @Schema(
                        example = "5",
                        description =
                                "Duracao da sessao em minutos. Ausente ou nulo aplica o padrao"
                                        + " de 1 minuto.")
                @Positive(message = "A duracao da sessao deve ser maior que zero.")
                @Max(value = 10_080, message = "A duracao da sessao nao pode passar de 7 dias.")
                Integer duracaoMinutos) {}
