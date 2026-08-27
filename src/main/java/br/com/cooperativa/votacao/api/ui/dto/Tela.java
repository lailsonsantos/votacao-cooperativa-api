package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Descricao de tela devolvida ao cliente, no formato do Anexo 1 do enunciado.
 *
 * <p>Este e o contrato central da avaliacao: o cliente nao conhece "pauta",
 * "sessao" nem "voto", apenas sabe renderizar {@link TipoTela#FORMULARIO} e
 * {@link TipoTela#SELECAO}. Toda a navegacao e dirigida pelo servidor &mdash;
 * cada acao devolve a proxima tela.
 *
 * <p>Um unico record cobre os dois tipos, com campos nulos omitidos na
 * serializacao. A alternativa, uma hierarquia selada com serializacao
 * polimorfica, produziria exatamente o mesmo JSON ao custo de tres tipos a mais;
 * para duas variantes, nao se paga.
 *
 * @param tipo          tipo da tela
 * @param titulo        titulo exibido no topo
 * @param itens         itens da tela: {@link ItemTela} em FORMULARIO,
 *                      {@link ItemSelecao} em SELECAO
 * @param botaoOk       acao principal, apenas em FORMULARIO
 * @param botaoCancelar acao secundaria, apenas em FORMULARIO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Descricao de tela no formato do Anexo 1")
public record Tela(
        TipoTela tipo, String titulo, List<?> itens, Botao botaoOk, Botao botaoCancelar) {

    /**
     * Cria uma tela do tipo FORMULARIO.
     *
     * @param titulo        titulo da tela
     * @param itens         campos e textos a exibir
     * @param botaoOk       acao principal
     * @param botaoCancelar acao secundaria, pode ser nula
     * @return a tela correspondente
     */
    public static Tela formulario(
            String titulo, List<ItemTela> itens, Botao botaoOk, Botao botaoCancelar) {
        return new Tela(TipoTela.FORMULARIO, titulo, itens, botaoOk, botaoCancelar);
    }

    /**
     * Cria uma tela do tipo SELECAO.
     *
     * @param titulo titulo da tela
     * @param itens  opcoes disponiveis
     * @return a tela correspondente
     */
    public static Tela selecao(String titulo, List<ItemSelecao> itens) {
        return new Tela(TipoTela.SELECAO, titulo, itens, null, null);
    }
}
