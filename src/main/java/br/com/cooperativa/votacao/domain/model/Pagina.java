package br.com.cooperativa.votacao.domain.model;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos) {

    /** Copia defensiva das colecoes recebidas. */
    public Pagina {
        conteudo = conteudo == null ? List.of() : List.copyOf(conteudo);
    }

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
     * @param <R> tipo de destino
     * @param conversor funcao aplicada a cada item
     * @return uma pagina equivalente, com o conteudo convertido
     */
    public <R> Pagina<R> mapear(Function<T, R> conversor) {
        return new Pagina<>(
                conteudo.stream().map(conversor).toList(), pagina, tamanho, totalElementos);
    }
}
