package br.com.cooperativa.votacao.api.ui.dto.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum TipoItem {
    TEXTO(1, "Texto"),
    INPUT_TEXTO(2, "Campo de texto"),
    INPUT_NUMERO(3, "Campo numérico"),
    INPUT_DATA(4, "Campo data");

    private final int id;
    private final String descricao;

    TipoItem(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public static TipoItem porId(int id) {
        return Arrays.stream(values())
                .filter(t -> t.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de item inválido: " + id));
    }
}
