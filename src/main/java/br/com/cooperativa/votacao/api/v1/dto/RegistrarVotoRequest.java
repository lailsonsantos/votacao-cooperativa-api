package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Corpo da requisicao de registro de voto.
 *
 * <p>A validacao do CPF usa {@link CPF}, do Hibernate Validator, que ja
 * acompanha o {@code spring-boot-starter-validation}. A restricao confere os
 * digitos verificadores e aceita o numero com ou sem pontuacao, o que dispensa
 * escrever o algoritmo a mao e faz a recusa acontecer na borda, antes de
 * qualquer acesso a banco ou chamada remota.
 *
 * <p>Declarar a regra aqui tambem melhora a resposta de erro: em vez de uma
 * mensagem generica, o cliente recebe o nome do campo que foi recusado.
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
                @CPF(message = "O CPF informado nao e valido.")
                String associadoId,
        @Schema(example = "SIM", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "A opcao de voto e obrigatoria (SIM ou NAO).")
                OpcaoVoto opcao) {}
