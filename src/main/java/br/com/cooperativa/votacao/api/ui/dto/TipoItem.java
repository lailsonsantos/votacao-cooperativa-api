package br.com.cooperativa.votacao.api.ui.dto;

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
