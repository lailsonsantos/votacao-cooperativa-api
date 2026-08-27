package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.Voto;
import java.util.UUID;

public interface VotoService {

    /**
     * Registra o voto de um associado em uma pauta.
     *
     * @param pautaId identificador da pauta em votação
     * @param cpf CPF do associado, já validado
     * @param opção opção escolhida
     * @return o voto persistido
     * @throws SessaoNaoAbertaException se a pauta não tiver sessão
     * @throws SessaoEncerradaException se a sessão já tiver fechado
     * @throws VotoDuplicadoException se o associado já votou nesta pauta
     * @throws AssociadoNaoAutorizadoException se o associado não estiver apto a votar
     */
    Voto registrar(UUID pautaId, Cpf cpf, OpcaoVoto opcao);

    /**
     * Indica se o associado já votou na pauta.
     *
     * @param pautaId identificador da pauta
     * @param cpf CPF do associado
     * @return {@code true} se já existir voto do associado na sessão da pauta
     */
    boolean jaVotou(UUID pautaId, Cpf cpf);
}
