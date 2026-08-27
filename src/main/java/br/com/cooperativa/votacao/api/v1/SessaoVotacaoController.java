package br.com.cooperativa.votacao.api.v1;

import br.com.cooperativa.votacao.api.v1.dto.request.AbrirSessaoRequest;
import br.com.cooperativa.votacao.api.v1.dto.response.SessaoResponse;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ApiV1
@Tag(name = "Sessões", description = "Abertura e consulta das sessões de votação")
@RequiredArgsConstructor
public class SessaoVotacaoController {
    private final SessaoVotacaoService sessaoService;
    private final Clock clock;

    @PostMapping("/pautas/{id}/sessao")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Abre a sessão de votação de uma pauta",
            description = "Quando a duração não é informada, a sessão fica aberta por 1 minuto.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sessão aberta"),
        @ApiResponse(responseCode = "404", description = "Pauta inexistente", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "A pauta já possui sessão",
                content = @Content)
    })
    public SessaoResponse abrir(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AbrirSessaoRequest request) {
        var duracao = request != null ? request.duracaoMinutos() : null;
        return SessaoResponse.de(sessaoService.abrir(id, duracao), clock.instant());
    }

    @GetMapping("/pautas/{id}/sessao")
    @Operation(summary = "Consulta a sessão de votação de uma pauta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão encontrada"),
        @ApiResponse(
                responseCode = "409",
                description = "A pauta ainda não teve sessão aberta",
                content = @Content)
    })
    public SessaoResponse consultar(@PathVariable UUID id) {
        return SessaoResponse.de(sessaoService.buscarObrigatoria(id), clock.instant());
    }
}
