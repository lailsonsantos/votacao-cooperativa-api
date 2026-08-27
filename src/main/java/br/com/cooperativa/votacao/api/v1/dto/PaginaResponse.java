package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.Pagina;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginacao da API.
 *
 * <p>Existe para dar um contrato explicito e estavel a paginacao na resposta. A serializacao de
 * tipos internos de biblioteca nao tem garantia entre versoes, e amarrar o contrato publico da API
 * a um detalhe de framework criaria uma quebra silenciosa em um upgrade de dependencia.
 *
 * @param <T> tipo do conteudo da pagina
 * @param conteudo itens da pagina atual
 * @param pagina indice da pagina, iniciando em zero
 * @param tamanho quantidade de itens por pagina
 * @param totalElementos total de itens em todas as paginas
 * @param totalPaginas quantidade de paginas
 * @param ultima {@code true} se esta e a ultima pagina
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
     * Copia defensiva das colecoes recebidas.
     *
     * <p>Um record e imutavel apenas na superficie: sem a copia, quem construiu a lista continua
     * podendo altera-la depois, e o objeto "imutavel" muda pelas costas de quem o recebeu. A
     * analise estatica sinaliza exatamente isso.
     */
    public PaginaResponse {
        conteudo = conteudo == null ? List.of() : List.copyOf(conteudo);
    }

    /**
     * Converte uma pagina do Spring Data aplicando um mapeador ao conteudo.
     *
     * @param <E> tipo da entidade de origem
     * @param <T> tipo do DTO de destino
     * @param pagina pagina de dominio
     * @param mapeador conversor de entidade para DTO
     * @return o envelope de paginacao correspondente
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
