package br.com.cooperativa.votacao.domain.model;

import br.com.cooperativa.votacao.domain.exception.CpfInvalidoException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public record Cpf(String numero) {
    /** Quantidade de digitos de um CPF. */
    private static final int TAMANHO = 11;

    /** Validador compartilhado. */
    private static final Validator VALIDADOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Portador da restricao, usado apenas para submeter o valor ao validador.
     *
     * @param valor CPF candidato, somente digitos
     */
    private record Candidato(@org.hibernate.validator.constraints.br.CPF String valor) {}

    /**
     * Normaliza e valida o numero na construcao.
     *
     * @throws CpfInvalidoException se o valor nao for um CPF valido
     */
    public Cpf {
        numero = somenteDigitos(numero);
        if (!VALIDADOR.validate(new Candidato(numero)).isEmpty()) {
            throw new CpfInvalidoException(numero);
        }
    }

    /**
     * Cria um CPF a partir de texto com ou sem formatacao.
     *
     * @param valor CPF digitado pelo associado, com ou sem pontos e traco
     * @return o objeto de valor validado
     * @throws CpfInvalidoException se o valor nao for um CPF valido
     */
    public static Cpf de(String valor) {
        return new Cpf(valor);
    }

    /**
     * Devolve o CPF mascarado, apto a aparecer em log.
     *
     * @return o CPF no formato {@code 198******69}
     */
    public String mascarado() {
        return mascarar(numero);
    }

    /**
     * Aplica a mascara de log a um CPF em formato texto.
     *
     * @param valor CPF em texto, possivelmente nulo
     * @return o valor mascarado, ou o proprio valor se for curto demais para mascarar
     */
    public static String mascarar(String valor) {
        if (valor == null || valor.length() < TAMANHO) {
            return valor;
        }
        return valor.substring(0, 3) + "******" + valor.substring(9);
    }

    /**
     * Remove qualquer caractere que nao seja digito.
     *
     * @param valor texto de entrada
     * @return apenas os digitos, ou string vazia se a entrada for nula
     */
    private static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
