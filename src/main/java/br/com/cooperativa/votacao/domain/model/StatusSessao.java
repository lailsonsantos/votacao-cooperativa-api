package br.com.cooperativa.votacao.domain.model;

public enum StatusSessao {
    /** A sessao ainda aceita votos. */
    ABERTA,

    /** O prazo da sessao expirou e nenhum voto novo e aceito. */
    FECHADA
}
