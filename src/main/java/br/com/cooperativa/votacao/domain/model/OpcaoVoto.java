package br.com.cooperativa.votacao.domain.model;

/**
 * Opcoes disponiveis para um voto.
 *
 * <p>O enunciado restringe o voto a "Sim" ou "Nao". Modelar como enum, e nao
 * como texto livre, faz o compilador e o desserializador rejeitarem qualquer
 * outro valor antes que ele chegue ao banco.
 */
public enum OpcaoVoto {

    /** Voto favoravel a pauta. */
    SIM,

    /** Voto contrario a pauta. */
    NAO
}
