package br.com.cooperativa.votacao.domain.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ResultadoApuracao {
    APROVADA(1, "APROVADA"),
    REPROVADA(2, "REPROVADA"),
    EMPATE(3, "EMPATE"),
    SEM_VOTOS(4, "Nenhum voto registrado");

    private final int id;
    private final String descricao;

    ResultadoApuracao(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public static ResultadoApuracao porId(int id) {
        return Arrays.stream(values())
                .filter(r -> r.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Resultado invalido: " + id));
    }
}
