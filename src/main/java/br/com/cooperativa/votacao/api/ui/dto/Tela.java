package br.com.cooperativa.votacao.api.ui.dto;

import br.com.cooperativa.votacao.api.ui.dto.enums.TipoTela;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Descrição de tela no formato do Anexo 1")
public record Tela(
        TipoTela tipo, String titulo, List<?> itens, Botao botaoOk, Botao botaoCancelar) {

    /** Copia defensiva das coleções recebidas. */
    public Tela {
        itens = itens == null ? null : List.copyOf(itens);
    }

    /**
     * Cria uma tela do tipo FORMULARIO.
     *
     * @param titulo titulo da tela
     * @param itens campos e textos a exibir
     * @param botaoOk ação principal
     * @param botaoCancelar ação secundaria, pode ser nula
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
     * @param itens opções disponíveis
     * @return a tela correspondente
     */
    public static Tela selecao(String titulo, List<ItemSelecao> itens) {
        return new Tela(TipoTela.SELECAO, titulo, itens, null, null);
    }
}
