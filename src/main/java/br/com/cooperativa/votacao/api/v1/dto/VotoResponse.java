package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import br.com.cooperativa.votacao.domain.model.Voto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Confirmacao de um voto registrado.
 *
 * @param id          identificador do voto
 * @param pautaId     pauta em que o voto foi registrado
 * @param associadoId CPF mascarado do associado
 * @param opcao       opcao escolhida
 * @param criadoEm    momento do registro, em UTC
 */
@Schema(description = "Confirmacao do voto registrado")
public record VotoResponse(
        UUID id, UUID pautaId, String associadoId, OpcaoVoto opcao, Instant criadoEm) {

    /**
     * Converte a entidade para a representacao da API.
     *
     * <p>O CPF volta <strong>mascarado</strong>. O associado ja sabe o proprio
     * numero e nao precisa recebe-lo de volta; devolver o dado completo apenas
     * ampliaria a exposicao de dado pessoal em caches, logs de proxy e historico
     * de navegador.
     *
     * @param voto entidade de origem
     * @return a representacao correspondente
     */
    public static VotoResponse de(Voto voto) {
        return new VotoResponse(
                voto.getId(),
                voto.getSessao().getPauta().getId(),
                Cpf.mascarar(voto.getAssociadoId()),
                voto.getOpcao(),
                voto.getCriadoEm());
    }
}
