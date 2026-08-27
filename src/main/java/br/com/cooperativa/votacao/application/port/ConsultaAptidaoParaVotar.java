package br.com.cooperativa.votacao.application.port;

import br.com.cooperativa.votacao.domain.model.Cpf;
import java.util.Optional;

public interface ConsultaAptidaoParaVotar {

    /**
     * Consulta se o associado está apto a votar.
     *
     * @param cpf CPF já validado do associado
     * @return a aptidão encontrada, ou vazio se o associado for desconhecido
     */
    Optional<AptidaoParaVotar> consultar(Cpf cpf);

    /**
     * Resposta da consulta de aptidão.
     *
     * @param podeVotar se o associado está habilitado a votar
     */
    record AptidaoParaVotar(boolean podeVotar) {}
}
