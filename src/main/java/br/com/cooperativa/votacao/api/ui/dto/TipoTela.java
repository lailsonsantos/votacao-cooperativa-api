package br.com.cooperativa.votacao.api.ui.dto;

import java.util.Arrays;

public enum TipoTela {
    FORMULARIO(1, "Formulario"),
    SELECAO(2, "Selecao");

    private final int id;
    private final String descricao;

    TipoTela(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public static TipoTela porId(int id) {
        return Arrays.stream(values())
                .filter(t -> t.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de tela invalido: " + id));
    }
}
