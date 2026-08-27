package br.com.cooperativa.votacao.domain.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum TipoErro {
    ENTRADA_INVALIDA(1, "Entrada inválida"),
    NAO_ENCONTRADO(2, "Recurso não encontrado"),
    CONFLITO(3, "Conflito com o estado atual"),
    REGRA_VIOLADA(4, "Regra de negócio violada");

    private final int id;
    private final String descricao;

    TipoErro(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public static TipoErro porId(int id) {
        return Arrays.stream(values())
                .filter(t -> t.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de erro inválido: " + id));
    }
}
