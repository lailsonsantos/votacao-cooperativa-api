package br.com.cooperativa.votacao.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sessao_votacao")
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessaoVotacao {
    /** Identificador da sessao, gerado pela aplicacao. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Pauta em deliberacao. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false, updatable = false, unique = true)
    private Pauta pauta;

    /** Instante de abertura, em UTC. */
    @Column(name = "abertura_em", nullable = false, updatable = false)
    private Instant aberturaEm;

    /** Instante em que a sessao para de aceitar votos, em UTC. */
    @Column(name = "fechamento_em", nullable = false, updatable = false)
    private Instant fechamentoEm;

    /**
     * Construtor de dominio.
     *
     * @param pauta pauta em deliberacao
     * @param aberturaEm instante de abertura
     * @param fechamentoEm instante de encerramento
     */
    private SessaoVotacao(Pauta pauta, Instant aberturaEm, Instant fechamentoEm) {
        this.id = UUID.randomUUID();
        this.pauta = pauta;
        this.aberturaEm = aberturaEm;
        this.fechamentoEm = fechamentoEm;
    }

    /**
     * Abre uma sessao com a duracao informada.
     *
     * @param pauta pauta que sera votada
     * @param agora instante de abertura, vindo do relogio injetado
     * @param duracao janela de votacao; o servico ja aplicou o default de 1 minuto quando a chamada
     *     nao informa duracao
     * @return a nova sessao, ainda nao persistida
     */
    public static SessaoVotacao abrir(Pauta pauta, Instant agora, Duration duracao) {
        return new SessaoVotacao(pauta, agora, agora.plus(duracao));
    }

    /**
     * Indica se a sessao ainda aceita votos.
     *
     * @param agora instante de referencia, injetado para permitir teste deterministico
     * @return {@code true} enquanto {@code agora} for anterior ao fechamento
     */
    public boolean estaAberta(Instant agora) {
        return agora.isBefore(fechamentoEm);
    }

    /**
     * Traduz a situacao da sessao para o enum exposto na API.
     *
     * @param agora instante de referencia
     * @return {@link StatusSessao#ABERTA} ou {@link StatusSessao#FECHADA}
     */
    public StatusSessao status(Instant agora) {
        return estaAberta(agora) ? StatusSessao.ABERTA : StatusSessao.FECHADA;
    }

    /**
     * Calcula quanto tempo resta de votacao.
     *
     * @param agora instante de referencia
     * @return o tempo restante, ou zero se a sessao ja encerrou
     */
    public Duration tempoRestante(Instant agora) {
        var restante = Duration.between(agora, fechamentoEm);
        return restante.isNegative() ? Duration.ZERO : restante;
    }
}
