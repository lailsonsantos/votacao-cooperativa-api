package br.com.cooperativa.votacao.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cooperativa.votacao.IntegracaoTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("CORS")
class CorsIT extends IntegracaoTest {

    private static final String ORIGEM_PERMITIDA = "http://localhost:5173";

    private static final String ORIGEM_RECUSADA = "https://origem-nao-declarada.example";

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("rota válida devolve os cabeçalhos para origem declarada")
    void rotaValida() throws Exception {
        mockMvc.perform(get("/api/v1/telas").header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                                        "X-Correlation-Id"));
    }

    @Test
    @DisplayName("preflight de POST é autorizado para origem declarada")
    void preflight() throws Exception {
        mockMvc.perform(
                        options("/api/v1/telas/pautas")
                                .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("origem não declarada é recusada")
    void origemRecusada() throws Exception {
        mockMvc.perform(get("/api/v1/telas").header(HttpHeaders.ORIGIN, ORIGEM_RECUSADA))
                .andExpect(status().isForbidden());
    }

    /**
     * Regressão: um {@code GET} em rota que só aceita {@code POST} é resolvido pelo tratador de
     * exceções, fora do fluxo normal de <em>handler mapping</em>. Com o CORS configurado pelo MVC,
     * a resposta saía sem {@code Access-Control-Allow-Origin} e o navegador a bloqueava — o cliente
     * via erro de rede em vez da tela de erro que o servidor havia montado.
     *
     * @throws Exception se a requisição falhar
     */
    @Test
    @DisplayName("método não suportado devolve a tela de erro COM os cabeçalhos de CORS")
    void metodoNaoSuportadoMantemCors() throws Exception {
        var rotaApenasPost = "/api/v1/telas/pautas/%s/votos".formatted(UUID.randomUUID());

        mockMvc.perform(get(rotaApenasPost).header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("rota inexistente devolve a tela de erro com os cabeçalhos de CORS")
    void rotaInexistenteMantemCors() throws Exception {
        mockMvc.perform(
                        get("/api/v1/telas/rota-que-nao-existe")
                                .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA));
    }
}
