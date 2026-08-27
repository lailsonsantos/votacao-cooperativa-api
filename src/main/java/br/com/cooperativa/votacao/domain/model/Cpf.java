package br.com.cooperativa.votacao.domain.model;

import br.com.cooperativa.votacao.domain.exception.CpfInvalidoException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Objeto de valor que representa um CPF valido.
 *
 * <p>Encapsular o CPF em um tipo proprio, em vez de trafegar {@code String}, garante que qualquer
 * CPF que circule pelo dominio ja passou pela validacao. Isso evita a chamada remota ao servico
 * externo para um numero que jamais poderia existir, devolvendo o erro mais cedo e sem gastar rede.
 *
 * <p>A validacao delega ao {@link org.hibernate.validator.constraints.br.CPF} do Hibernate
 * Validator, que ja acompanha o {@code spring-boot-starter-validation}. Reimplementar o calculo dos
 * digitos verificadores a mao seria reescrever, com menos testes, algo que a biblioteca padrao da
 * plataforma ja resolve.
 *
 * <p>A camada de telas nao pode usar Bean Validation por anotacao, porque recebe um mapa aberto de
 * campos ({@code AcaoTelaRequest}); e por isso que a validacao tambem existe aqui, e nao apenas nos
 * DTOs de entrada da API REST.
 *
 * @param numero os onze digitos do CPF, sem pontuacao
 */
public record Cpf(String numero) {
    /** Quantidade de digitos de um CPF. */
    private static final int TAMANHO = 11;

    /**
     * Validador compartilhado.
     *
     * <p>Construir a fabrica e caro e o objeto e thread-safe, entao uma unica instancia estatica
     * atende toda a aplicacao. Criar uma por chamada tornaria o registro de voto sensivelmente mais
     * lento sob a carga da Tarefa Bonus 2.
     */
    private static final Validator VALIDADOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Portador da restricao, usado apenas para submeter o valor ao validador.
     *
     * <p>Bean Validation valida propriedades de um objeto, nao valores soltos. Este record existe
     * para dar ao numero um lugar onde a anotacao possa ser declarada.
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
     * <p>CPF e dado pessoal sob a LGPD; registrar o numero completo em arquivo de log criaria uma
     * base de dados pessoais paralela, fora de qualquer controle de acesso. A mascara preserva o
     * suficiente para correlacionar registros durante uma investigacao.
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
     * <p>A normalizacao acontece antes da validacao para que o associado possa digitar o CPF com ou
     * sem pontuacao, e para que o valor persistido tenha sempre a mesma forma — do contrario, a
     * constraint de unicidade do voto deixaria o mesmo associado votar duas vezes usando formatos
     * diferentes.
     *
     * @param valor texto de entrada
     * @return apenas os digitos, ou string vazia se a entrada for nula
     */
    private static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
