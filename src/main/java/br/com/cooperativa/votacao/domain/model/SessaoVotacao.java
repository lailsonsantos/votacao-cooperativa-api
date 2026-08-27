package br.com.cooperativa.votacao.domain.model;

import br.com.cooperativa.votacao.domain.enums.StatusSessao;
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
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false, updatable = false, unique = true)
    private Pauta pauta;

    @Column(name = "abertura_em", nullable = false, updatable = false)
    private Instant aberturaEm;

    @Column(name = "fechamento_em", nullable = false, updatable = false)
    private Instant fechamentoEm;

    private SessaoVotacao(Pauta pauta, Instant aberturaEm, Instant fechamentoEm) {
        this.id = UUID.randomUUID();
        this.pauta = pauta;
        this.aberturaEm = aberturaEm;
        this.fechamentoEm = fechamentoEm;
    }

    public static SessaoVotacao abrir(Pauta pauta, Instant agora, Duration duracao) {
        return new SessaoVotacao(pauta, agora, agora.plus(duracao));
    }

    public boolean estaAberta(Instant agora) {
        return agora.isBefore(fechamentoEm);
    }

    public StatusSessao status(Instant agora) {
        return estaAberta(agora) ? StatusSessao.ABERTA : StatusSessao.FECHADA;
    }

    public Duration tempoRestante(Instant agora) {
        var restante = Duration.between(agora, fechamentoEm);
        return restante.isNegative() ? Duration.ZERO : restante;
    }
}
