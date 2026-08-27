package br.com.cooperativa.votacao.api.v1;

import br.com.cooperativa.votacao.api.v1.dto.RegistrarVotoRequest;
import br.com.cooperativa.votacao.api.v1.dto.ResultadoResponse;
import br.com.cooperativa.votacao.api.v1.dto.VotoResponse;
import br.com.cooperativa.votacao.application.ResultadoService;
import br.com.cooperativa.votacao.application.VotoService;
import br.com.cooperativa.votacao.domain.model.Cpf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Endpoints de registro de voto e apuracao de resultado. */
@ApiV1
@Tag(name = "Votos", description = "Registro de votos e apuracao do resultado")
@RequiredArgsConstructor
public class VotoController {
    private final VotoService votoService;
    private final ResultadoService resultadoService;

    /**
     * Registra o voto de um associado.
     *
     * @param id identificador da pauta
     * @param request CPF do associado e opcao escolhida
     * @return {@code 201} com a confirmacao do voto
     */
    @PostMapping("/pautas/{id}/votos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registra o voto de um associado",
            description = "Cada associado pode votar uma unica vez por pauta.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Voto registrado"),
        @ApiResponse(responseCode = "400", description = "CPF invalido", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Voto duplicado ou sessao inexistente",
                content = @Content),
        @ApiResponse(
                responseCode = "422",
                description = "Sessao encerrada ou associado nao autorizado",
                content = @Content)
    })
    public VotoResponse votar(
            @PathVariable UUID id, @Valid @RequestBody RegistrarVotoRequest request) {
        var voto = votoService.registrar(id, Cpf.de(request.associadoId()), request.opcao());
        return VotoResponse.de(voto);
    }

    /**
     * Apura o resultado de uma pauta.
     *
     * <p>A consulta e permitida com a sessao aberta; nesse caso a resposta vem marcada como
     * parcial.
     *
     * @param id identificador da pauta
     * @return {@code 200} com o resultado apurado
     */
    @GetMapping("/pautas/{id}/resultado")
    @Operation(
            summary = "Apura o resultado de uma pauta",
            description =
                    "Com a sessao ainda aberta, a resposta vem com parcial=true e o numero"
                            + " pode mudar ate o fechamento.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultado apurado"),
        @ApiResponse(
                responseCode = "409",
                description = "A pauta ainda nao teve sessao aberta",
                content = @Content)
    })
    public ResultadoResponse resultado(@PathVariable UUID id) {
        return ResultadoResponse.de(resultadoService.apurar(id));
    }
}
