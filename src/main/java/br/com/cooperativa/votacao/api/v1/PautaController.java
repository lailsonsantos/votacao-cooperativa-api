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
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Endpoints de cadastro e consulta de pautas.
 *
 * <p>O controlador nao contem regra de negocio: valida a entrada, delega ao
 * servico e traduz o retorno. E o que permite que a camada de telas
 * (Server-Driven UI) reutilize exatamente os mesmos casos de uso sem duplicacao.
 */
@ApiV1
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
        @ApiResponse(responseCode = "400", description = "Dados invalidos", content = @io.swagger.v3.oas.annotations.media.Content)
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
            @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PaginaResponse.de(pautaService.listar(pageable), PautaResponse::de);
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
        @ApiResponse(responseCode = "404", description = "Pauta inexistente", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public PautaResponse buscar(@PathVariable UUID id) {
        return PautaResponse.de(pautaService.buscar(id));
    }
}
