package br.com.cooperativa.votacao.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expoe o relogio da aplicacao como um bean injetavel.
 *
 * <p>Nenhuma classe de dominio ou de servico chama {@code Instant.now()}
 * diretamente. Injetar o {@link Clock} permite que os testes substituam o tempo
 * por um valor fixo e verifiquem o encerramento de uma sessao sem
 * {@code Thread.sleep}, o que mantem a suite rapida e deterministica.
 */
@Configuration
public class ClockConfig {

    /**
     * Relogio de sistema em UTC.
     *
     * <p>UTC em toda a aplicacao elimina a classe inteira de bugs de fuso
     * horario e horario de verao; a conversao para o fuso do usuario acontece
     * apenas na borda de apresentacao.
     *
     * @return o relogio usado por toda a aplicacao
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
