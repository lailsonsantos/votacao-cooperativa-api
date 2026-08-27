package br.com.cooperativa.votacao.domain.model;

import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "voto")
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voto {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false, updatable = false)
    private SessaoVotacao sessao;

    @Column(name = "associado_id", nullable = false, length = 11, updatable = false)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "opcao", nullable = false, length = 3, updatable = false)
    private OpcaoVoto opcao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    private Voto(SessaoVotacao sessao, String associadoId, OpcaoVoto opcao, Instant criadoEm) {
        this.id = UUID.randomUUID();
        this.sessao = sessao;
        this.associadoId = associadoId;
        this.opcao = opcao;
        this.criadoEm = criadoEm;
    }

    public static Voto registrar(
            SessaoVotacao sessao, String associadoId, OpcaoVoto opcao, Instant agora) {
        return new Voto(sessao, associadoId, opcao, agora);
    }
}
