package br.com.cooperativa.votacao.api.error;

import br.com.cooperativa.votacao.api.ui.builder.TelaFactory;
import br.com.cooperativa.votacao.api.ui.dto.Tela;
import br.com.cooperativa.votacao.config.CorrelationIdFilter;
import br.com.cooperativa.votacao.domain.exception.NegocioException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz excecoes em respostas HTTP coerentes com a superficie acionada.
 *
 * <p>A aplicacao tem dois publicos com necessidades opostas diante de um erro:
 *
 * <ul>
 *   <li><strong>{@code /api/v1/**}</strong> &mdash; consumidores de API esperam
 *       o status HTTP correto e um corpo no padrao RFC 7807
 *       ({@link ProblemDetail}), que e o formato nativo do Spring 6;
 *   <li><strong>{@code /api/v1/telas/**}</strong> &mdash; o cliente do Anexo 1
 *       sabe renderizar telas, nao codigos de status. Um {@code 409} cru o
 *       deixaria sem nada para desenhar, entao o erro vira uma tela legivel com
 *       caminho de volta.
 * </ul>
 *
 * <p>Nenhuma regra de negocio vive aqui: cada excecao ja carrega seu status e
 * seu tipo, e o tratador apenas os transporta. Uma regra nova nao exige alterar
 * esta classe.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Prefixo das rotas que devolvem telas em vez de ProblemDetail. */
    private static final String PREFIXO_TELAS = "/api/v1/telas";

    /** Base dos identificadores de tipo de erro publicados na documentacao. */
    private static final String BASE_TIPO = "https://api.cooperativa.com/erros/";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final TelaFactory telas;
    private final Clock clock;

    /**
     * Cria o tratador.
     *
     * @param telas fabrica usada para montar a tela de erro
     * @param clock relogio injetado, para carimbar o instante do erro
     */
    public GlobalExceptionHandler(TelaFactory telas, Clock clock) {
        this.telas = telas;
        this.clock = clock;
    }

    /**
     * Trata as falhas previstas pelas regras de negocio.
     *
     * <p>Registradas em {@code WARN}, nunca em {@code ERROR}: um voto duplicado
     * recusado significa que a aplicacao funcionou. Poluir o nivel de erro com
     * rejeicoes esperadas tornaria o alarme inutil.
     *
     * @param e       excecao de negocio
     * @param request requisicao que originou a falha
     * @return ProblemDetail ou tela de erro, conforme a superficie acionada
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Object> tratarNegocio(NegocioException e, HttpServletRequest request) {
        log.warn("Regra de negocio violada: [{}] {}", e.getTipo(), e.getMessage());

        if (ehTela(request)) {
            return ResponseEntity.ok(telaDeErro(e.getTitulo(), e.getMessage()));
        }

        return ResponseEntity.status(e.getStatus())
                .body(problema(e.getStatus(), e.getTipo(), e.getTitulo(), e.getMessage(), request));
    }

    /**
     * Trata falhas de validacao de Bean Validation.
     *
     * @param e       excecao com os campos invalidos
     * @param request requisicao que originou a falha
     * @return {@code 400} detalhando cada campo recusado
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> tratarValidacao(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        var detalhe =
                e.getBindingResult().getFieldErrors().stream()
                        .map(erro -> "%s: %s".formatted(erro.getField(), erro.getDefaultMessage()))
                        .collect(Collectors.joining(" "));

        log.warn("Requisicao invalida: {}", detalhe);

        if (ehTela(request)) {
            return ResponseEntity.ok(telaDeErro("Dados invalidos", detalhe));
        }

        return ResponseEntity.badRequest()
                .body(
                        problema(
                                HttpStatus.BAD_REQUEST,
                                "requisicao-invalida",
                                "Requisicao invalida",
                                detalhe,
                                request));
    }

    /**
     * Trata corpo malformado e enums desconhecidos.
     *
     * <p>Um valor fora do enum {@code OpcaoVoto} chega como falha de leitura do
     * corpo, e nao como violacao de validacao; sem este tratamento viraria
     * {@code 500} para um erro que e claramente do cliente.
     *
     * @param e       excecao de leitura
     * @param request requisicao que originou a falha
     * @return {@code 400} com mensagem generica e segura
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> tratarCorpoInvalido(Exception e, HttpServletRequest request) {
        var detalhe =
                "Nao foi possivel interpretar a requisicao. Verifique os campos e os valores"
                        + " enviados (a opcao de voto deve ser SIM ou NAO).";

        log.warn("Corpo de requisicao invalido: {}", e.getMessage());

        if (ehTela(request)) {
            return ResponseEntity.ok(telaDeErro("Dados invalidos", detalhe));
        }

        return ResponseEntity.badRequest()
                .body(
                        problema(
                                HttpStatus.BAD_REQUEST,
                                "requisicao-invalida",
                                "Requisicao invalida",
                                detalhe,
                                request));
    }

    /**
     * Rede de seguranca para violacoes de integridade nao traduzidas antes.
     *
     * @param e       violacao de integridade
     * @param request requisicao que originou a falha
     * @return {@code 409} sem expor detalhes do banco
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> tratarIntegridade(
            DataIntegrityViolationException e, HttpServletRequest request) {

        log.warn("Violacao de integridade nao traduzida: {}", e.getMostSpecificCause().getMessage());
        var detalhe = "A operacao conflita com dados ja registrados.";

        if (ehTela(request)) {
            return ResponseEntity.ok(telaDeErro("Conflito", detalhe));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        problema(
                                HttpStatus.CONFLICT,
                                "conflito-de-dados",
                                "Conflito de dados",
                                detalhe,
                                request));
    }

    /**
     * Ultimo recurso para falhas inesperadas.
     *
     * <p>Este e o unico tratador que registra em {@code ERROR}, com o rastro
     * completo. A resposta ao cliente e deliberadamente generica: detalhes
     * internos em mensagem de erro sao vazamento de informacao. O
     * {@code correlationId} devolvido permite localizar o rastro no log.
     *
     * @param e       excecao inesperada
     * @param request requisicao que originou a falha
     * @return {@code 500} com mensagem generica
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarInesperado(Exception e, HttpServletRequest request) {
        log.error("Falha inesperada ao processar {} {}", request.getMethod(), request.getRequestURI(), e);

        var detalhe =
                "Ocorreu um erro inesperado. Informe o identificador de correlacao ao suporte.";

        if (ehTela(request)) {
            return ResponseEntity.ok(telaDeErro("Erro inesperado", detalhe));
        }

        return ResponseEntity.internalServerError()
                .body(
                        problema(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "erro-inesperado",
                                "Erro inesperado",
                                detalhe,
                                request));
    }

    /**
     * Indica se a requisicao veio da superficie de telas.
     *
     * @param request requisicao em tratamento
     * @return {@code true} se a rota pertencer a {@code /api/v1/telas}
     */
    private boolean ehTela(HttpServletRequest request) {
        return request.getRequestURI().startsWith(PREFIXO_TELAS);
    }

    /**
     * Monta a tela de erro apresentada ao cliente do Anexo 1.
     *
     * @param titulo   titulo da tela
     * @param mensagem explicacao ao usuario
     * @return a tela de erro
     */
    private Tela telaDeErro(String titulo, String mensagem) {
        return telas.erro(titulo, mensagem);
    }

    /**
     * Monta o corpo de erro no padrao RFC 7807.
     *
     * @param status   status HTTP
     * @param tipo     identificador estavel do erro
     * @param titulo   titulo curto
     * @param detalhe  explicacao ao consumidor
     * @param request  requisicao que originou a falha
     * @return o ProblemDetail preenchido
     */
    private ProblemDetail problema(
            HttpStatus status,
            String tipo,
            String titulo,
            String detalhe,
            HttpServletRequest request) {

        var problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create(BASE_TIPO + tipo));
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("timestamp", clock.instant());

        // Devolver o correlationId fecha o ciclo de diagnostico: quem recebe o
        // erro consegue localizar o rastro completo da requisicao no log.
        var correlationId = CorrelationIdFilter.atual();
        if (correlationId != null) {
            problema.setProperty("correlationId", correlationId);
        }

        return problema;
    }
}
