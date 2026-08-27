package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Item de uma tela do tipo {@link TipoTela#FORMULARIO}.
 *
 * <p>O Anexo 1 usa um unico formato de objeto para todos os tipos de item,
 * variando apenas quais campos estao presentes. Campos nulos sao omitidos da
 * serializacao para que o JSON produzido seja identico ao dos exemplos do
 * enunciado &mdash; um item {@code TEXTO} nao deve carregar {@code "id": null}.
 *
 * @param tipo   tipo do item
 * @param texto  conteudo, presente apenas em itens {@link TipoItem#TEXTO}
 * @param id     identificador do campo; e a chave com que o valor digitado sera
 *               enviado no corpo do POST da acao
 * @param titulo rotulo exibido acima do campo de entrada
 * @param valor  valor inicial do campo, texto ou numero conforme o tipo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemTela(TipoItem tipo, String texto, String id, String titulo, Object valor) {
    /**
     * Cria um item de texto somente leitura.
     *
     * @param texto conteudo a exibir
     * @return o item correspondente
     */
    public static ItemTela texto(String texto) {
        return new ItemTela(TipoItem.TEXTO, texto, null, null, null);
    }

    /**
     * Cria um campo de entrada de texto.
     *
     * @param id     chave com que o valor sera enviado no POST
     * @param titulo rotulo do campo
     * @param valor  valor inicial, pode ser nulo
     * @return o item correspondente
     */
    public static ItemTela inputTexto(String id, String titulo, String valor) {
        return new ItemTela(TipoItem.INPUT_TEXTO, null, id, titulo, valor);
    }

    /**
     * Cria um campo de entrada numerica.
     *
     * @param id     chave com que o valor sera enviado no POST
     * @param titulo rotulo do campo
     * @param valor  valor inicial
     * @return o item correspondente
     */
    public static ItemTela inputNumero(String id, String titulo, Number valor) {
        return new ItemTela(TipoItem.INPUT_NUMERO, null, id, titulo, valor);
    }

    /**
     * Cria um campo de entrada de data.
     *
     * @param id     chave com que o valor sera enviado no POST
     * @param titulo rotulo do campo
     * @param valor  valor inicial no formato {@code dd/MM/yyyy}
     * @return o item correspondente
     */
    public static ItemTela inputData(String id, String titulo, String valor) {
        return new ItemTela(TipoItem.INPUT_DATA, null, id, titulo, valor);
    }
}
