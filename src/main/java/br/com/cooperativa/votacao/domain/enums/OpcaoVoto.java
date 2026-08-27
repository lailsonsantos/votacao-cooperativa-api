package br.com.cooperativa.votacao.domain.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum OpcaoVoto {
    SIM(1, "Sim"),
    NAO(2, "Nao");

    private final int id;
    private final String descricao;

    OpcaoVoto(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public static OpcaoVoto porId(int id) {
        return Arrays.stream(values())
                .filter(o -> o.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Opcao de voto invalida: " + id));
    }
}
