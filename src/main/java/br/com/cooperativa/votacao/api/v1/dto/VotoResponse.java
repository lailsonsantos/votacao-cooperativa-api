package br.com.cooperativa.votacao.api.v1.dto;

import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.Voto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Confirmacao do voto registrado")
public record VotoResponse(
        UUID id, UUID pautaId, String associadoId, OpcaoVoto opcao, Instant criadoEm) {
    /**
     * Converte a entidade para a representacao da API.
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
