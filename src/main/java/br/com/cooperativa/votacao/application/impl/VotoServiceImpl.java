package br.com.cooperativa.votacao.application.impl;

import br.com.cooperativa.votacao.application.AssociadoValidator;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import br.com.cooperativa.votacao.application.VotoService;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import br.com.cooperativa.votacao.domain.model.Voto;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao do registro de voto.
 *
 * <p>O contrato vive em {@link VotoService}; aqui documenta-se apenas o porque
 * de cada escolha &mdash; e a mais importante delas e delegar a unicidade ao
 * banco em vez de checar antes de gravar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VotoServiceImpl implements VotoService {

    private final VotoRepository votoRepository;
    private final SessaoVotacaoService sessaoService;
    private final AssociadoValidator associadoValidator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>A unicidade do voto e garantida pela constraint
     * {@code uk_voto_sessao_associado}, e nao por consulta previa: sob a
     * concorrencia prevista na Tarefa Bonus 2, um {@code SELECT} seguido de
     * {@code INSERT} abre uma janela entre a verificacao e a gravacao na qual
     * duas requisicoes simultaneas do mesmo associado passariam ambas pela
     * checagem. Delegar ao banco tambem elimina uma ida de rede por voto.
     */
    @Override
    @Transactional
    public Voto registrar(UUID pautaId, Cpf cpf, OpcaoVoto opcao) {
        var sessao = sessaoService.buscarObrigatoria(pautaId);

        // O instante vem do Clock injetado, e nao de Instant.now(), para que os
        // testes consigam simular o fim da sessao sem depender de tempo real.
        var agora = clock.instant();
        if (!sessao.estaAberta(agora)) {
            log.warn(
                    "Voto recusado: sessao encerrada. pautaId={} associado={}",
                    pautaId,
                    cpf.mascarado());
            throw new SessaoEncerradaException(pautaId);
        }

        associadoValidator.validarPodeVotar(cpf);

        try {
            var voto =
                    votoRepository.salvarEConfirmar(
                            Voto.registrar(sessao, cpf.numero(), opcao, agora));

            log.info(
                    "Voto registrado. pautaId={} sessaoId={} associado={} opcao={}",
                    pautaId,
                    sessao.getId(),
                    cpf.mascarado(),
                    opcao);
            return voto;

        } catch (DataIntegrityViolationException e) {
            // Unico caminho seguro para detectar duplicidade: a constraint do banco.
            // A excecao de infraestrutura e traduzida em erro de negocio na fronteira,
            // para que a camada de API nao precise conhecer detalhes de persistencia.
            log.warn("Voto recusado: duplicado. pautaId={} associado={}", pautaId, cpf.mascarado());
            throw new VotoDuplicadoException(pautaId, cpf.numero(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public boolean jaVotou(UUID pautaId, Cpf cpf) {
        return sessaoService
                .buscar(pautaId)
                .map(sessao -> votoRepository.existeVotoDoAssociado(sessao.getId(), cpf.numero()))
                .orElse(false);
    }
}
