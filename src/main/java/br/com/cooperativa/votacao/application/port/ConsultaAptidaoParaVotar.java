package br.com.cooperativa.votacao.application.port;

import br.com.cooperativa.votacao.domain.model.Cpf;
import java.util.Optional;

/**
 * Porta de saida para a consulta de aptidao de um associado.
 *
 * <p>A camada de aplicacao declara <em>o que</em> precisa saber; a
 * infraestrutura decide <em>como</em> descobrir. Antes desta interface, o
 * validador importava diretamente o cliente HTTP do servico externo, o que
 * invertia a direcao correta das dependencias: uma regra de negocio passava a
 * depender de um detalhe de integracao.
 *
 * <p>Consequencia pratica: se a cooperativa passar a manter um cadastro proprio
 * de associados, basta uma implementacao nova desta porta. Nenhuma regra muda, e
 * nenhum teste de regra precisa ser reescrito.
 *
 * <p>O nome descreve o papel, e nao a tecnologia. Chamar a porta de
 * {@code UserInfoClient} amarraria a abstracao ao fornecedor atual, que e
 * exatamente o acoplamento que ela existe para evitar.
 */
public interface ConsultaAptidaoParaVotar {

    /**
     * Consulta se o associado esta apto a votar.
     *
     * @param cpf CPF ja validado do associado
     * @return a aptidao encontrada, ou vazio se o associado for desconhecido
     */
    Optional<AptidaoParaVotar> consultar(Cpf cpf);

    /**
     * Resposta da consulta de aptidao.
     *
     * <p>Expressa apenas a decisao que interessa ao dominio. O enum
     * {@code ABLE_TO_VOTE}/{@code UNABLE_TO_VOTE} do servico externo permanece na
     * infraestrutura, onde e um detalhe do contrato daquele fornecedor.
     *
     * @param podeVotar se o associado esta habilitado a votar
     */
    record AptidaoParaVotar(boolean podeVotar) {}
}
