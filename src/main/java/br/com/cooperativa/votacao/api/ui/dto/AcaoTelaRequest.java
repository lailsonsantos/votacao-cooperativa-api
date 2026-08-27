package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Corpo generico das acoes disparadas pelas telas.
 *
 * <p>Segundo o Anexo 1, o cliente envia o {@code body} do botao acrescido dos valores digitados,
 * indexados pelo {@code id} de cada campo. O conjunto de chaves varia conforme a tela, entao um DTO
 * fixo por acao obrigaria a criar um tipo novo a cada tela &mdash; e a quebrar quando uma tela
 * ganhasse um campo.
 *
 * <p>Este mapa aberto absorve qualquer combinacao, e os acessores tipados abaixo concentram a
 * conversao em um unico lugar, com mensagens de erro claras.
 */
public class AcaoTelaRequest {
    /** Campos recebidos do cliente, na forma bruta. */
    private final Map<String, Object> campos = new HashMap<>();

    /**
     * Recebe qualquer propriedade presente no JSON.
     *
     * @param chave nome do campo
     * @param valor valor informado
     */
    @JsonAnySetter
    public void adicionar(String chave, Object valor) {
        campos.put(chave, valor);
    }

    /**
     * Expoe os campos recebidos.
     *
     * @return o mapa de campos, na forma bruta
     */
    @JsonAnyGetter
    public Map<String, Object> getCampos() {
        // Vista imutavel: quem recebe o mapa nao deve conseguir alterar o estado
        // interno. Nao se usa Map.copyOf porque valores nulos sao possiveis em um
        // corpo JSON arbitrario, e aquele metodo os rejeita.
        return Collections.unmodifiableMap(campos);
    }

    /**
     * Le um campo como texto.
     *
     * @param chave nome do campo
     * @return o valor em texto, ou {@code null} se ausente ou vazio
     */
    public String texto(String chave) {
        var valor = campos.get(chave);
        if (valor == null) {
            return null;
        }
        var texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    /**
     * Le um campo numerico inteiro.
     *
     * <p>Aceita tanto numero quanto texto, porque o cliente pode enviar o valor de um {@code
     * INPUT_NUMERO} em qualquer das duas formas dependendo de como o campo foi preenchido.
     *
     * @param chave nome do campo
     * @return o valor inteiro, ou {@code null} se ausente ou nao numerico
     */
    public Integer inteiro(String chave) {
        var valor = campos.get(chave);
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        var texto = texto(chave);
        if (texto == null) {
            return null;
        }
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
