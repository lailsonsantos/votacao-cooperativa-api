package br.com.cooperativa.votacao.application.impl;

import br.com.cooperativa.votacao.application.AssociadoValidator;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import br.com.cooperativa.votacao.application.VotoService;
import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.Voto;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotoServiceImpl implements VotoService {

    private final VotoRepository votoRepository;
    private final SessaoVotacaoService sessaoService;
    private final AssociadoValidator associadoValidator;
    private final Clock clock;

    @Override
    @Transactional
    public Voto registrar(UUID pautaId, Cpf cpf, OpcaoVoto opcao) {
        var sessao = sessaoService.buscarObrigatoria(pautaId);

        // Clock injetado em vez de Instant.now() para dar pra testar o fim da sessão.
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
            // Só a constraint do banco detecta duplicidade com segurança. Traduzo aqui
            // pra API não precisar saber de detalhe de persistência.
            log.warn("Voto recusado: duplicado. pautaId={} associado={}", pautaId, cpf.mascarado());
            throw new VotoDuplicadoException(pautaId, cpf.numero(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean jaVotou(UUID pautaId, Cpf cpf) {
        return sessaoService
                .buscar(pautaId)
                .map(sessao -> votoRepository.existeVotoDoAssociado(sessao.getId(), cpf.numero()))
                .orElse(false);
    }
}
