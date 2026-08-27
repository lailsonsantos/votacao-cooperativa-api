package br.com.cooperativa.votacao.api.ui.dto;

/**
 * Tipos de item que podem compor uma tela {@link TipoTela#FORMULARIO}.
 *
 * <p>Conjunto fechado, exatamente o documentado no Anexo 1. Se o catalogo do cliente crescer, novos
 * valores entram aqui e a camada de telas passa a emiti-los &mdash; sem alterar dominio nem API
 * REST.
 */
public enum TipoItem {
    /** Texto somente leitura. */
    TEXTO,

    /** Campo de entrada de texto. */
    INPUT_TEXTO,

    /** Campo de entrada numerica. */
    INPUT_NUMERO,

    /** Campo de entrada de data, no formato {@code dd/MM/yyyy}. */
    INPUT_DATA
}
