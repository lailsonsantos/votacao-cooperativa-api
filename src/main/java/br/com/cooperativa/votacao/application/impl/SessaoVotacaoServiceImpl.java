package br.com.cooperativa.votacao.application.impl;

import br.com.cooperativa.votacao.application.PautaService;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import br.com.cooperativa.votacao.config.AppProperties;
import br.com.cooperativa.votacao.domain.exception.SessaoJaAbertaException;
import br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.repository.SessaoVotacaoRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessaoVotacaoServiceImpl implements SessaoVotacaoService {

    private final SessaoVotacaoRepository sessaoRepository;
    private final PautaService pautaService;
    private final AppProperties appProperties;
    private final Clock clock;

    @Override
    @Transactional
    public SessaoVotacao abrir(UUID pautaId, Integer duracaoMinutos) {
        var pauta = pautaService.buscar(pautaId);

        // Checagem só pra dar a mensagem certa. Quem garante mesmo é a constraint
        // uk_sessão_pauta, tratada no catch abaixo.
        if (sessaoRepository.existePorPauta(pautaId)) {
            throw new SessaoJaAbertaException(pautaId);
        }

        var minutos =
                duracaoMinutos != null
                        ? duracaoMinutos
                        : appProperties.sessao().duracaoPadraoMinutos();

        var sessao = SessaoVotacao.abrir(pauta, clock.instant(), Duration.ofMinutes(minutos));

        try {
            var salva = sessaoRepository.salvarEConfirmar(sessao);
            log.info(
                    "Sessao de votacao aberta. pautaId={} sessaoId={} duracaoMinutos={} "
                            + "fechamentoEm={}",
                    pautaId,
                    salva.getId(),
                    minutos,
                    salva.getFechamentoEm());
            return salva;
        } catch (DataIntegrityViolationException e) {
            throw new SessaoJaAbertaException(pautaId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SessaoVotacao buscarObrigatoria(UUID pautaId) {
        return buscar(pautaId).orElseThrow(() -> new SessaoNaoAbertaException(pautaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessaoVotacao> buscar(UUID pautaId) {
        return sessaoRepository.buscarPorPauta(pautaId);
    }
}
