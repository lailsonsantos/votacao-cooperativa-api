package br.com.cooperativa.votacao.api.ui.builder;

import br.com.cooperativa.votacao.config.AppProperties;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Monta as URLs absolutas usadas nas telas.
 *
 * <p>Atende diretamente a dica do enunciado: <em>"Deixe o dominio das URLs de
 * callback passiveis de alteracao via configuracao, para facilitar o teste tanto
 * no emulador, quanto em dispositivos fisicos."</em>
 *
 * <p>Toda URL de tela passa por aqui. Concentrar a montagem em um unico
 * componente e o que permite trocar emulador, dispositivo fisico e nuvem
 * mudando uma unica variavel de ambiente &mdash; e o que garante que nenhuma URL
 * escape com {@code localhost} embutido no codigo.
 */
@Component
public class UrlTelaFactory {

    /** Prefixo comum de todas as rotas de tela. */
    private static final String RAIZ = "/api/v1/telas";

    private final AppProperties appProperties;

    /**
     * Cria a fabrica.
     *
     * @param appProperties configuracao que contem a URL base publica
     */
    public UrlTelaFactory(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

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
     * URL do formulario de cadastro de pauta.
     *
     * @return a URL absoluta do formulario
     */
    public String novaPauta() {
        return absoluta("/pautas/nova");
    }

    /**
     * URL de criacao de pauta (destino do botao do formulario).
     *
     * @return a URL absoluta da acao
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
     * URL de abertura de sessao de uma pauta.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta da acao
     */
    public String abrirSessao(UUID pautaId) {
        return absoluta("/pautas/" + pautaId + "/sessao");
    }

    /**
     * URL de identificacao do associado antes de votar.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta da acao
     */
    public String identificacao(UUID pautaId) {
        return absoluta("/pautas/" + pautaId + "/votos/identificacao");
    }

    /**
     * URL de registro de voto.
     *
     * @param pautaId identificador da pauta
     * @return a URL absoluta da acao
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
