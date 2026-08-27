package br.com.cooperativa.votacao.domain.model;

/**
 * Desfecho da apuracao de uma pauta.
 *
 * <p>{@link #EMPATE} e {@link #SEM_VOTOS} existem como valores proprios em vez de serem embutidos
 * em {@code REPROVADA}: sao situacoes distintas do ponto de vista da assembleia e tratar as tres
 * como "nao aprovada" esconderia informacao de quem consome a API.
 */
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
