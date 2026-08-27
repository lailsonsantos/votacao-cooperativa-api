package br.com.cooperativa.votacao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class RelogioDeTeste extends Clock {

    /** Instante que o relogio reporta no momento. */
    private Instant agora;

    /** Fuso reportado, sempre UTC, como no restante da aplicacao. */
    private final ZoneId zona = ZoneId.of("UTC");

    /** Cria o relogio posicionado no instante atual do sistema. */
    public RelogioDeTeste() {
        this.agora = Instant.now();
    }

    /**
     * Adianta o relogio.
     *
     * @param duracao quanto avancar
     */
    public void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    /** Reposiciona o relogio no instante atual do sistema. */
    public void reiniciar() {
        agora = Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
