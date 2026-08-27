package br.com.cooperativa.votacao.domain.model;

/**
 * Situacao de uma sessao de votacao em relacao ao momento presente.
 *
 * <p>O status nao e persistido: e derivado da comparacao entre o instante atual
 * e o fechamento da sessao. Ver {@link SessaoVotacao#status(java.time.Instant)}.
 */
public enum StatusSessao {

    /** A sessao ainda aceita votos. */
    ABERTA,

    /** O prazo da sessao expirou e nenhum voto novo e aceito. */
    FECHADA
}
