package br.com.cooperativa.votacao.domain.model;

public enum ResultadoApuracao {
    /** Mais votos "Sim" do que "Nao". */
    APROVADA,

    /** Mais votos "Nao" do que "Sim". */
    REPROVADA,

    /** Mesma quantidade de votos "Sim" e "Nao", com ao menos um voto registrado. */
    EMPATE,

    /** Nenhum voto foi registrado na sessao. */
    SEM_VOTOS
}
