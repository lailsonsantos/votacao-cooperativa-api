package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import br.com.cooperativa.votacao.domain.model.Voto;
import java.util.UUID;

public interface VotoService {

    /**
     * Registra o voto de um associado em uma pauta.
     *
     * @param pautaId identificador da pauta em votacao
     * @param cpf CPF do associado, ja validado
     * @param opcao opcao escolhida
     * @return o voto persistido
     * @throws SessaoNaoAbertaException se a pauta nao tiver sessao
     * @throws SessaoEncerradaException se a sessao ja tiver fechado
     * @throws VotoDuplicadoException se o associado ja votou nesta pauta
     * @throws AssociadoNaoAutorizadoException se o associado nao estiver apto a votar
     */
    Voto registrar(UUID pautaId, Cpf cpf, OpcaoVoto opcao);

    /**
     * Indica se o associado ja votou na pauta.
     *
     * @param pautaId identificador da pauta
     * @param cpf CPF do associado
     * @return {@code true} se ja existir voto do associado na sessao da pauta
     */
    boolean jaVotou(UUID pautaId, Cpf cpf);
}
