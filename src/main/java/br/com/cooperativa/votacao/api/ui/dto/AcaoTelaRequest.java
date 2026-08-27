package br.com.cooperativa.votacao.api.ui.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        // Vista imutavel. Nao uso Map.copyOf porque ele rejeita valor nulo, e um
        // corpo JSON qualquer pode ter.
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
