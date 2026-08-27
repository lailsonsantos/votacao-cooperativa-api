package br.com.cooperativa.votacao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Relogio que pode ser adiantado durante um teste.
 *
 * <p>Permite exercitar o estado "sessao encerrada" sem esperar o prazo real passar. Sem isso,
 * cobrir esse caminho custaria um minuto de espera por teste — e uma suite que demora deixa de ser
 * executada.
 *
 * <p>E a contrapartida, em teste de integracao, da decisao de injetar o {@link Clock} em vez de
 * chamar {@code Instant.now()} pelo codigo.
 */
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

    /**
     * Reposiciona o relogio no instante atual do sistema.
     *
     * <p>Chamado entre testes para que o avanco de um nao contamine o seguinte.
     */
    public void reiniciar() {
        agora = Instant.now();
    }

    /** {@inheritDoc} */
    @Override
    public ZoneId getZone() {
        return zona;
    }

    /** {@inheritDoc} */
    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Instant instant() {
        return agora;
    }
}
