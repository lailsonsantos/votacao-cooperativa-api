package br.com.cooperativa.votacao.domain.model;

import br.com.cooperativa.votacao.domain.exception.CpfInvalidoException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public record Cpf(String numero) {

    private static final int TAMANHO = 11;

    private static final Validator VALIDADOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private record Candidato(@org.hibernate.validator.constraints.br.CPF String valor) {}

    public Cpf {
        numero = somenteDigitos(numero);
        if (!VALIDADOR.validate(new Candidato(numero)).isEmpty()) {
            throw new CpfInvalidoException(numero);
        }
    }

    public static Cpf de(String valor) {
        return new Cpf(valor);
    }

    public String mascarado() {
        return mascarar(numero);
    }

    public static String mascarar(String valor) {
        if (valor == null || valor.length() < TAMANHO) {
            return valor;
        }
        return valor.substring(0, 3) + "******" + valor.substring(9);
    }

    private static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
