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

/**
 * Assunto submetido a deliberacao da assembleia.
 *
 * <p>A entidade JPA e usada diretamente como modelo de dominio, sem um modelo
 * espelho. E um <em>trade-off</em> consciente, registrado em
 * {@code docs/adr/0001-entidade-jpa-como-modelo-de-dominio.md}: aceita-se o
 * acoplamento a JPA em troca de eliminar uma camada inteira de mapeamento que,
 * para tres agregados, seria cerimonia sem contrapartida.
 *
 * <p>A identidade e baseada apenas no identificador: dois objetos que
 * representam a mesma linha do banco sao a mesma entidade, ainda que um deles
 * tenha sido carregado antes de uma alteracao. Nao ha {@code @ToString} de
 * proposito &mdash; em {@code Voto} o metodo gerado incluiria o CPF, e qualquer
 * log que imprimisse a entidade vazaria dado pessoal.
 */
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
     * @param titulo    titulo da pauta
     * @param descricao descricao opcional
     * @param criadaEm  instante do cadastro
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
     * <p>O identificador e gerado pela aplicacao, e nao pelo banco, para que o
     * servico conheca o id antes do {@code flush} &mdash; isso permite montar a
     * URL de resposta e registrar o log sem uma ida extra ao banco.
     *
     * @param titulo    titulo da pauta, ja validado na borda
     * @param descricao descricao opcional
     * @param agora     instante de criacao, vindo do relogio injetado
     * @return a nova pauta, ainda nao persistida
     */
    public static Pauta criar(String titulo, String descricao, Instant agora) {
        return new Pauta(titulo, descricao, agora);
    }
}
