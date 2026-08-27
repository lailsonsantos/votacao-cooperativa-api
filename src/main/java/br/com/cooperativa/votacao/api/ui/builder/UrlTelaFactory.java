package br.com.cooperativa.votacao.api.ui.builder;

import br.com.cooperativa.votacao.config.AppProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlTelaFactory {
    /** Prefixo comum de todas as rotas de tela. */
    private static final String RAIZ = "/api/v1/telas";

    private final AppProperties appProperties;

    /**
     * URL do menu inicial.
     *
     * @return a URL absoluta do menu
     */
    public String menu() {
        return absoluta("");
    }

    /**
     * URL da listagem de pautas.
     *
     * @return a URL absoluta da lista
     */
    public String listaPautas() {
        return absoluta("/pautas");
    }

    /**
     * URL do formulário de cadastro de pauta.
     *
     * @return a URL absoluta do formulário
     */
    public String novaPauta() {
        return absoluta("/pautas/nova");
    }

    /**
     * URL de criação de pauta (destino do botão do formulário).
     *
     * @return a URL absoluta da ação
     */
    public String criarPauta() {
        return absoluta("/pautas");
    }

    /**
     * URL da tela de detalhe de uma pauta.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta do detalhe
     */
    public String pauta(UUID pautaId) {
        return absoluta("/pautas/" + pautaId);
    }

    /**
     * URL de abertura de sessão de uma pauta.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta da ação
     */
    public String abrirSessao(UUID pautaId) {
        return absoluta("/pautas/" + pautaId + "/sessao");
    }

    /**
     * URL de identificação do associado antes de votar.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta da ação
     */
    public String identificacao(UUID pautaId) {
        return absoluta("/pautas/" + pautaId + "/votos/identificacao");
    }

    /**
     * URL de registro de voto.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta da ação
     */
    public String votar(UUID pautaId) {
        return absoluta("/pautas/" + pautaId + "/votos");
    }

    /**
     * URL da tela de resultado.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta do resultado
     */
    public String resultado(UUID pautaId) {
        return absoluta("/pautas/" + pautaId + "/resultado");
    }

    /**
     * Prefixa um caminho com a URL base configurada.
     *
     * @param caminho caminho relativo dentro da raiz de telas
     * @return a URL absoluta correspondente
     */
    private String absoluta(String caminho) {
        return appProperties.callback().baseUrlNormalizada() + RAIZ + caminho;
    }
}
