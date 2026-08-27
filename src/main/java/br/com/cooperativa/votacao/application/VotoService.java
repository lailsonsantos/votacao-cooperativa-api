package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.exception.VotoDuplicadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import br.com.cooperativa.votacao.domain.model.Voto;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de registro de voto.
 */
@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    private final VotoRepository votoRepository;
    private final SessaoVotacaoService sessaoService;
    private final AssociadoValidator associadoValidator;
    private final Clock clock;

    /**
     * Cria o servico.
     *
     * @param votoRepository     acesso aos votos
     * @param sessaoService      consulta de sessoes
     * @param associadoValidator verificacao do direito de voto
     * @param clock              relogio injetado
     */
    public VotoService(
            VotoRepository votoRepository,
            SessaoVotacaoService sessaoService,
            AssociadoValidator associadoValidator,
            Clock clock) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
        this.associadoValidator = associadoValidator;
        this.clock = clock;
    }

    /**
     * Registra o voto de um associado em uma pauta.
     *
     * <p>A unicidade do voto e garantida pela constraint
     * {@code uk_voto_sessao_associado}, e nao por consulta previa: sob a
     * concorrencia prevista na Tarefa Bonus 2, um {@code SELECT} seguido de
     * {@code INSERT} abre uma janela entre a verificacao e a gravacao na qual duas
     * requisicoes simultaneas do mesmo associado passariam ambas pela checagem.
     * Delegar ao banco tambem elimina uma ida de rede por voto, o que importa em
     * um cenario de centenas de milhares de registros.
     *
     * @param pautaId identificador da pauta em votacao
     * @param cpf     CPF do associado, ja validado nos digitos verificadores
     * @param opcao   opcao escolhida
     * @return o voto persistido
     * @throws br.com.cooperativa.votacao.domain.exception.SessaoNaoAbertaException
     *         se a pauta nao tiver sessao
     * @throws SessaoEncerradaException se a sessao ja tiver fechado
     * @throws VotoDuplicadoException   se o associado ja votou nesta pauta
     * @throws br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException
     *         se o servico externo negar o CPF
     */
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
                    votoRepository.saveAndFlush(
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
            log.warn(
                    "Voto recusado: duplicado. pautaId={} associado={}",
                    pautaId,
                    cpf.mascarado());
            throw new VotoDuplicadoException(pautaId, cpf.numero(), e);
        }
    }

    /**
     * Indica se o associado ja votou na pauta.
     *
     * <p>Usado apenas pelas telas, para nao oferecer a opcao de voto a quem ja
     * votou. Nao substitui a constraint: e uma cortesia de interface, nao uma
     * garantia de integridade.
     *
     * @param pautaId identificador da pauta
     * @param cpf     CPF do associado
     * @return {@code true} se ja existir voto do associado na sessao da pauta
     */
    @Transactional(readOnly = true)
    public boolean jaVotou(UUID pautaId, Cpf cpf) {
        return sessaoService
                .buscar(pautaId)
                .map(
                        sessao ->
                                votoRepository.existsBySessaoIdAndAssociadoId(
                                        sessao.getId(), cpf.numero()))
                .orElse(false);
    }
}
