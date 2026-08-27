package br.com.cooperativa.votacao.domain.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum StatusSessao {
    ABERTA(1, "Aberta"),
    FECHADA(2, "Fechada");

    private final int id;
    private final String descricao;

    StatusSessao(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public static StatusSessao porId(int id) {
        return Arrays.stream(values())
                .filter(s -> s.id == id)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Status de sessao invalido: " + id));
    }
}
