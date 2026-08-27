package br.com.cooperativa.votacao.api.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Schema(description = "Parametros de abertura da sessão de votação")
public record AbrirSessaoRequest(
        @Schema(
                        example = "5",
                        description =
                                "Duração da sessão em minutos. Ausente ou nulo aplica o padrão"
                                        + " de 1 minuto.")
                @Positive(message = "A duração da sessão deve ser maior que zero.")
                @Max(value = 10_080, message = "A duração da sessão não pode passar de 7 dias.")
                Integer duracaoMinutos) {}
