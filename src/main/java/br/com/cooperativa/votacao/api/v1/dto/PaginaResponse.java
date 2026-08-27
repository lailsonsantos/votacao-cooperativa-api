package br.com.cooperativa.votacao.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Envelope de paginacao da API.
 *
 * <p>Existe para nao expor {@code org.springframework.data.domain.Page} na
 * resposta: a serializacao daquele tipo nao tem contrato estavel entre versoes
 * do Spring Data, e amarrar o contrato publico da API a um detalhe interno do
 * framework criaria uma quebra silenciosa em um upgrade de dependencia.
 *
 * @param <T>            tipo do conteudo da pagina
 * @param conteudo       itens da pagina atual
 * @param pagina         indice da pagina, iniciando em zero
 * @param tamanho        quantidade de itens por pagina
 * @param totalElementos total de itens em todas as paginas
 * @param totalPaginas   quantidade de paginas
 * @param ultima         {@code true} se esta e a ultima pagina
 */
@Schema(description = "Pagina de resultados")
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean ultima) {
    /**
     * Converte uma pagina do Spring Data aplicando um mapeador ao conteudo.
     *
     * @param <E>      tipo da entidade de origem
     * @param <T>      tipo do DTO de destino
     * @param page     pagina de origem
     * @param mapeador conversor de entidade para DTO
     * @return o envelope de paginacao correspondente
     */
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapeador) {
        return new PaginaResponse<>(
                page.getContent().stream().map(mapeador).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
