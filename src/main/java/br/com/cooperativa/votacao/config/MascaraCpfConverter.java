package br.com.cooperativa.votacao.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

public class MascaraCpfConverter extends MessageConverter {

    private static final Pattern CPF =
            Pattern.compile("(?<!\\d)(\\d{3})\\.?(\\d{3})\\.?(\\d{3})-?(\\d{2})(?!\\d)");

    @Override
    public String convert(ILoggingEvent evento) {
        var mensagem = super.convert(evento);
        if (mensagem == null || mensagem.isEmpty()) {
            return mensagem;
        }
        return CPF.matcher(mensagem).replaceAll("$1******$4");
    }
}
