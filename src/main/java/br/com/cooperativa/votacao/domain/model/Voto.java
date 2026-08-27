package br.com.cooperativa.votacao.domain.model;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Manifestacao de um associado sobre uma pauta.
 *
 * <p>A regra "um voto por associado por pauta" e garantida pela constraint
 * unica {@code uk_voto_sessao_associado}, e nao por uma consulta previa na
 * aplicacao. Sob a concorrencia prevista na Tarefa Bonus 2, um {@code SELECT}
 * seguido de {@code INSERT} abre uma janela entre a verificacao e a gravacao na
 * qual duas requisicoes simultaneas passariam pela checagem.
 */
@Entity
@Table(name = "voto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voto {

    /** Identificador do voto, gerado pela aplicacao. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Sessao em que o voto foi registrado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false, updatable = false)
    private SessaoVotacao sessao;

    /**
     * Identificador unico do associado.
     *
     * <p>O enunciado fala em "id unico" no requisito base e em CPF na Tarefa
     * Bonus 1. Unificamos os dois no CPF para nao criar dois identificadores
     * concorrentes para a mesma pessoa. A premissa esta registrada no README.
     */
    @Column(name = "associado_id", nullable = false, length = 11, updatable = false)
    private String associadoId;

    /** Opcao escolhida pelo associado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "opcao", nullable = false, length = 3, updatable = false)
    private OpcaoVoto opcao;

    /** Momento do registro, em UTC. */
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Construtor de dominio.
     *
     * @param sessao      sessao em que o voto e registrado
     * @param associadoId CPF do associado, somente digitos
     * @param opcao       opcao escolhida
     * @param criadoEm    instante do registro
     */
    private Voto(SessaoVotacao sessao, String associadoId, OpcaoVoto opcao, Instant criadoEm) {
        this.id = UUID.randomUUID();
        this.sessao = sessao;
        this.associadoId = associadoId;
        this.opcao = opcao;
        this.criadoEm = criadoEm;
    }

    /**
     * Cria um voto pronto para persistencia.
     *
     * @param sessao      sessao aberta em que o voto sera registrado
     * @param associadoId CPF do associado, somente digitos
     * @param opcao       opcao escolhida
     * @param agora       instante do registro, vindo do relogio injetado
     * @return o novo voto, ainda nao persistido
     */
    public static Voto registrar(
            SessaoVotacao sessao, String associadoId, OpcaoVoto opcao, Instant agora) {
        return new Voto(sessao, associadoId, opcao, agora);
    }
}
