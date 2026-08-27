package br.com.cooperativa.votacao.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Fatia paginada de um conjunto de resultados.
 *
 * <p>Existe para que as portas de repositorio nao precisem falar
 * {@code org.springframework.data.domain.Page}. O dominio nao deve conhecer a
 * biblioteca de persistencia escolhida &mdash; nem mesmo para representar algo
 * tao generico quanto uma pagina.
 *
 * @param <T>            tipo do conteudo
 * @param conteudo       itens desta pagina
 * @param pagina         indice da pagina, iniciando em zero
 * @param tamanho        quantidade maxima de itens por pagina
 * @param totalElementos total de itens em todas as paginas
 */
public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos) {

    /**
     * Quantidade de paginas necessarias para percorrer todos os elementos.
     *
     * @return o total de paginas, ou zero quando nao ha elementos
     */
    public int totalPaginas() {
        return tamanho <= 0 ? 0 : (int) Math.ceil((double) totalElementos / tamanho);
    }

    /**
     * Indica se esta e a ultima pagina.
     *
     * @return {@code true} quando nao ha pagina seguinte
     */
    public boolean ultima() {
        return pagina >= totalPaginas() - 1;
    }

    /**
     * Indica se a pagina nao tem conteudo.
     *
     * @return {@code true} quando o conteudo esta vazio
     */
    public boolean vazia() {
        return conteudo.isEmpty();
    }

    /**
     * Converte o conteudo preservando os dados de paginacao.
     *
     * <p>Permite que a camada de API transforme entidades em DTOs sem
     * reconstruir a pagina campo a campo.
     *
     * @param <R>       tipo de destino
     * @param conversor funcao aplicada a cada item
     * @return uma pagina equivalente, com o conteudo convertido
     */
    public <R> Pagina<R> mapear(Function<T, R> conversor) {
        return new Pagina<>(conteudo.stream().map(conversor).toList(), pagina, tamanho, totalElementos);
    }
}
