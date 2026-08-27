package br.com.cooperativa.votacao.api.v1;

import br.com.cooperativa.votacao.api.v1.dto.CriarPautaRequest;
import br.com.cooperativa.votacao.api.v1.dto.PaginaResponse;
import br.com.cooperativa.votacao.api.v1.dto.PautaResponse;
import br.com.cooperativa.votacao.application.PautaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ApiV1
@Validated
@Tag(name = "Pautas", description = "Cadastro e consulta das pautas da assembleia")
@RequiredArgsConstructor
public class PautaController {
    private final PautaService pautaService;

    /**
     * Cadastra uma nova pauta.
     *
     * @param request dados da pauta
     * @return {@code 201} com a pauta criada e o cabecalho {@code Location}
     */
    @PostMapping("/pautas")
    @Operation(summary = "Cadastra uma nova pauta")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pauta cadastrada"),
        @ApiResponse(
                responseCode = "400",
                description = "Dados invalidos",
                content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody CriarPautaRequest request) {
        var pauta = pautaService.criar(request.titulo(), request.descricao());
        var location = URI.create("/api/v1/pautas/" + pauta.getId());
        return ResponseEntity.created(location).body(PautaResponse.de(pauta));
    }

    /**
     * Lista as pautas cadastradas, da mais recente para a mais antiga.
     *
     * @param pageable pagina solicitada; o tamanho padrao evita resposta ilimitada
     * @return {@code 200} com a pagina de pautas
     */
    @GetMapping("/pautas")
    @Operation(summary = "Lista as pautas cadastradas, de forma paginada")
    public PaginaResponse<PautaResponse> listar(
            @RequestParam(defaultValue = "0")
                    @Min(value = 0, message = "A pagina nao pode ser negativa.")
                    int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "O tamanho da pagina deve ser ao menos 1.")
                    @Max(value = 100, message = "O tamanho da pagina nao pode passar de 100.")
                    int size) {
        return PaginaResponse.de(pautaService.listar(page, size), PautaResponse::de);
    }

    /**
     * Detalha uma pauta.
     *
     * @param id identificador da pauta
     * @return {@code 200} com a pauta encontrada
     */
    @GetMapping("/pautas/{id}")
    @Operation(summary = "Detalha uma pauta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pauta encontrada"),
        @ApiResponse(
                responseCode = "404",
                description = "Pauta inexistente",
                content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public PautaResponse buscar(@PathVariable UUID id) {
        return PautaResponse.de(pautaService.buscar(id));
    }
}
