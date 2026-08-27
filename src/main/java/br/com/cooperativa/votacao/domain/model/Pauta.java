package br.com.cooperativa.votacao.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pauta")
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pauta {
    /** Identificador da pauta, gerado pela aplicacao. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Titulo apresentado ao associado nas telas de listagem e votacao. */
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    /** Texto explicativo do que esta em deliberacao. */
    @Column(name = "descricao", length = 2000)
    private String descricao;

    /** Momento do cadastro, em UTC. */
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    /**
     * Construtor de dominio.
     *
     * @param titulo titulo da pauta
     * @param descricao descricao opcional
     * @param criadaEm instante do cadastro
     */
    private Pauta(String titulo, String descricao, Instant criadaEm) {
        this.id = UUID.randomUUID();
        this.titulo = titulo;
        this.descricao = descricao;
        this.criadaEm = criadaEm;
    }

    /**
     * Cria uma nova pauta.
     *
     * @param titulo titulo da pauta, ja validado na borda
     * @param descricao descricao opcional
     * @param agora instante de criacao, vindo do relogio injetado
     * @return a nova pauta, ainda nao persistida
     */
    public static Pauta criar(String titulo, String descricao, Instant agora) {
        return new Pauta(titulo, descricao, agora);
    }
}
