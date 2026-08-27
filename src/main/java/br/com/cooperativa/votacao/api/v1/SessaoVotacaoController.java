package br.com.cooperativa.votacao.api.v1;

import br.com.cooperativa.votacao.api.v1.dto.AbrirSessaoRequest;
import br.com.cooperativa.votacao.api.v1.dto.SessaoResponse;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Endpoints de abertura e consulta da sessao de votacao de uma pauta.
 */
@ApiV1
@Tag(name = "Sessoes", description = "Abertura e consulta das sessoes de votacao")
public class SessaoVotacaoController {

    private final SessaoVotacaoService sessaoService;
    private final Clock clock;

    /**
     * Cria o controlador.
     *
     * @param sessaoService caso de uso de sessoes
     * @param clock         relogio injetado, usado para derivar status e tempo restante
     */
    public SessaoVotacaoController(SessaoVotacaoService sessaoService, Clock clock) {
        this.sessaoService = sessaoService;
        this.clock = clock;
    }

    /**
     * Abre a sessao de votacao de uma pauta.
     *
     * <p>O corpo e opcional: {@code POST} sem corpo abre a sessao com a duracao
     * padrao de 1 minuto, conforme o enunciado.
     *
     * @param id      identificador da pauta
     * @param request duracao solicitada, opcional
     * @return {@code 201} com os dados da sessao aberta
     */
    @PostMapping("/pautas/{id}/sessao")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Abre a sessao de votacao de uma pauta",
            description =
                    "Quando a duracao nao e informada, a sessao fica aberta por 1 minuto.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sessao aberta"),
        @ApiResponse(responseCode = "404", description = "Pauta inexistente", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "A pauta ja possui sessao",
                content = @Content)
    })
    public SessaoResponse abrir(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AbrirSessaoRequest request) {

        var duracao = request != null ? request.duracaoMinutos() : null;
        return SessaoResponse.de(sessaoService.abrir(id, duracao), clock.instant());
    }

    /**
     * Consulta a sessao de votacao de uma pauta.
     *
     * @param id identificador da pauta
     * @return {@code 200} com os dados da sessao
     */
    @GetMapping("/pautas/{id}/sessao")
    @Operation(summary = "Consulta a sessao de votacao de uma pauta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessao encontrada"),
        @ApiResponse(
                responseCode = "409",
                description = "A pauta ainda nao teve sessao aberta",
                content = @Content)
    })
    public SessaoResponse consultar(@PathVariable UUID id) {
        return SessaoResponse.de(sessaoService.buscarObrigatoria(id), clock.instant());
    }
}
