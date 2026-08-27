package br.com.cooperativa.votacao.api.ui.dto.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum TipoTela {
    FORMULARIO(1, "Formulário"),
    SELECAO(2, "Seleção");

    private final int id;
    private final String descricao;

    TipoTela(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public static TipoTela porId(int id) {
        return Arrays.stream(values())
                .filter(t -> t.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de tela inválido: " + id));
    }
}
