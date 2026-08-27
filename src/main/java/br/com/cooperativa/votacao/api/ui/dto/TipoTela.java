package br.com.cooperativa.votacao.api.ui.dto;

/** Tipos de tela suportados pelo cliente, conforme o Anexo 1 do enunciado. */
public enum TipoTela {
    /** Colecao de itens com um ou dois botoes de acao no rodape. */
    FORMULARIO,

    /** Lista de opcoes, cada uma com sua propria acao. */
    SELECAO
}
