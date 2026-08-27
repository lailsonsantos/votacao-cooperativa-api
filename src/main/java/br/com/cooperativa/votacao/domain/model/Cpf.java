package br.com.cooperativa.votacao.domain.model;

import br.com.cooperativa.votacao.domain.exception.CpfInvalidoException;

/**
 * Objeto de valor que representa um CPF valido.
 *
 * <p>Encapsular o CPF em um tipo proprio, em vez de trafegar {@code String},
 * garante que qualquer CPF que circule pelo dominio ja passou pela validacao dos
 * digitos verificadores. Isso evita a chamada remota ao servico externo para um
 * numero que jamais poderia existir, economizando rede e devolvendo o erro mais
 * cedo.
 *
 * @param numero os onze digitos do CPF, sem pontuacao
 */
public record Cpf(String numero) {

    /** Quantidade de digitos de um CPF. */
    private static final int TAMANHO = 11;

    /**
     * Valida o numero na construcao.
     *
     * @throws CpfInvalidoException se o valor nao for um CPF valido
     */
    public Cpf {
        numero = somenteDigitos(numero);
        if (!valido(numero)) {
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
     * <p>CPF e dado pessoal sob a LGPD; registrar o numero completo em arquivo de
     * log criaria uma base de dados pessoais paralela, fora de qualquer controle
     * de acesso. A mascara preserva o suficiente para correlacionar registros
     * durante uma investigacao.
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

    /**
     * Verifica os digitos verificadores segundo o algoritmo da Receita Federal.
     *
     * @param cpf CPF com exatamente onze digitos
     * @return {@code true} se o CPF for estruturalmente valido
     */
    private static boolean valido(String cpf) {
        if (cpf.length() != TAMANHO) {
            return false;
        }
        // Sequencias de digitos repetidos passam no calculo dos verificadores,
        // mas nao sao CPFs validos; precisam de checagem propria.
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
                && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
    }

    /**
     * Calcula um digito verificador do CPF.
     *
     * @param cpf       CPF completo
     * @param posicao   posicao do digito a calcular (9 para o primeiro, 10 para o segundo)
     * @return o digito verificador esperado
     */
    private static int digitoVerificador(String cpf, int posicao) {
        int soma = 0;
        int peso = posicao + 1;
        for (int i = 0; i < posicao; i++) {
            soma += (cpf.charAt(i) - '0') * peso--;
        }
        int resto = soma % TAMANHO;
        return resto < 2 ? 0 : TAMANHO - resto;
    }
}
