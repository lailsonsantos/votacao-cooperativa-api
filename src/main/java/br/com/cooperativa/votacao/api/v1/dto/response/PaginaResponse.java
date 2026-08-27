package br.com.cooperativa.votacao.api.v1.dto.response;

import br.com.cooperativa.votacao.domain.model.Pagina;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

@Schema(description = "Pagina de resultados")
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean ultima) {

    public PaginaResponse {
        conteudo = conteudo == null ? List.of() : List.copyOf(conteudo);
    }

    /**
     * Converte uma pagina do Spring Data aplicando um mapeador ao conteúdo.
     *
     * @param <E> tipo da entidade de origem
     * @param <T> tipo do DTO de destino
     * @param pagina pagina de domínio
     * @param mapeador conversor de entidade para DTO
     * @return o envelope de paginação correspondente
     */
    public static <E, T> PaginaResponse<T> de(Pagina<E> pagina, Function<E, T> mapeador) {
        var convertida = pagina.mapear(mapeador);
        return new PaginaResponse<>(
                convertida.conteudo(),
                convertida.pagina(),
                convertida.tamanho(),
                convertida.totalElementos(),
                convertida.totalPaginas(),
                convertida.ultima());
    }
}
