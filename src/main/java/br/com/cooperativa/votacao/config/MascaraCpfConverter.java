package br.com.cooperativa.votacao.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * Conversor do Logback que remove CPFs das mensagens antes da gravacao.
 *
 * <p>O codigo da aplicacao ja registra CPFs mascarados por
 * {@link br.com.cooperativa.votacao.domain.model.Cpf#mascarado()}, mas
 * bibliotecas de terceiros nao conhecem essa regra: o driver JDBC, por exemplo,
 * inclui o valor da coluna na mensagem de uma violacao de constraint. Este
 * conversor e a rede de seguranca que garante a mascara qualquer que seja a
 * origem da mensagem.
 *
 * <p>CPF e dado pessoal sob a LGPD. Registrar o numero completo em arquivo de
 * log criaria uma base de dados pessoais paralela, fora de qualquer controle de
 * acesso e de qualquer politica de retencao.
 */
public class MascaraCpfConverter extends MessageConverter {
    /**
     * Sequencias de 11 digitos, com ou sem a pontuacao usual de CPF.
     *
     * <p>As bordas {@code (?<!\d)} e {@code (?!\d)} evitam mascarar os 11
     * primeiros digitos de um numero maior, como um identificador de 15 digitos.
     */
    private static final Pattern CPF =
            Pattern.compile("(?<!\\d)(\\d{3})\\.?(\\d{3})\\.?(\\d{3})-?(\\d{2})(?!\\d)");

    /**
     * Aplica a mascara a mensagem do evento de log.
     *
     * @param evento evento de log em formatacao
     * @return a mensagem com os CPFs substituidos por {@code 123******89}
     */
    @Override
    public String convert(ILoggingEvent evento) {
        var mensagem = super.convert(evento);
        if (mensagem == null || mensagem.isEmpty()) {
            return mensagem;
        }
        return CPF.matcher(mensagem).replaceAll("$1******$4");
    }
}
