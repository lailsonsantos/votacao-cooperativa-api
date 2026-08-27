package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import br.com.cooperativa.votacao.domain.model.Cpf;
import java.util.UUID;

public class VotoDuplicadoException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param pautaId identificador da pauta
     * @param associadoId CPF do associado, mascarado na mensagem
     * @param causa violacao de integridade que originou a falha
     */
    public VotoDuplicadoException(UUID pautaId, String associadoId, Throwable causa) {
        super(
                TipoErro.CONFLITO,
                "voto-duplicado",
                "O associado %s ja registrou voto na pauta %s."
                        .formatted(Cpf.mascarar(associadoId), pautaId),
                causa);
    }

    @Override
    public String getTitulo() {
        return "Voto duplicado";
    }
}
