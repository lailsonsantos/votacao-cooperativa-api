package br.com.cooperativa.votacao.application.port;

import br.com.cooperativa.votacao.domain.model.Cpf;
import java.util.Optional;

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
     * @param podeVotar se o associado esta habilitado a votar
     */
    record AptidaoParaVotar(boolean podeVotar) {}
}
