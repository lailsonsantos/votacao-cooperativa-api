package br.com.cooperativa.votacao.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

public class MascaraCpfConverter extends MessageConverter {
    /** Sequencias de 11 digitos, com ou sem a pontuacao usual de CPF. */
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
