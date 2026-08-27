package br.com.cooperativa.votacao.domain.model;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos) {

    public Pagina {
        conteudo = conteudo == null ? List.of() : List.copyOf(conteudo);
    }

    public int totalPaginas() {
        return tamanho <= 0 ? 0 : (int) Math.ceil((double) totalElementos / tamanho);
    }

    public boolean ultima() {
        return pagina >= totalPaginas() - 1;
    }

    public boolean vazia() {
        return conteudo.isEmpty();
    }

    public <R> Pagina<R> mapear(Function<T, R> conversor) {
        return new Pagina<>(
                conteudo.stream().map(conversor).toList(), pagina, tamanho, totalElementos);
    }
}
