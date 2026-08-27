package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.model.Cpf;
import java.util.UUID;

/**
 * Lancada quando um associado tenta votar mais de uma vez na mesma pauta.
 *
 * <p>Traduz a violacao da constraint {@code uk_voto_sessao_associado} em erro de
 * negocio. A deteccao acontece no banco, e nao em consulta previa, porque so o
 * banco resolve corretamente a corrida entre duas requisicoes simultaneas do
 * mesmo associado.
 */
public class VotoDuplicadoException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param pautaId     identificador da pauta
     * @param associadoId CPF do associado, mascarado na mensagem
     * @param causa       violacao de integridade que originou a falha
     */
    public VotoDuplicadoException(UUID pautaId, String associadoId, Throwable causa) {
        super(
                TipoErro.CONFLITO,
                "voto-duplicado",
                "O associado %s ja registrou voto na pauta %s."
                        .formatted(Cpf.mascarar(associadoId), pautaId),
                causa);
    }

    /** {@inheritDoc} */
    @Override
    public String getTitulo() {
        return "Voto duplicado";
    }
}
