package br.com.cooperativa.votacao.infrastructure.integration.userinfo;

import java.util.Arrays;

public enum StatusAssociado {
    ABLE_TO_VOTE(1, "Apto a votar"),
    UNABLE_TO_VOTE(2, "Impedido de votar");

    private final int id;
    private final String descricao;

    StatusAssociado(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusAssociado porId(int id) {
        return Arrays.stream(values())
                .filter(s -> s.id == id)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Status de associado inválido: " + id));
    }
}
