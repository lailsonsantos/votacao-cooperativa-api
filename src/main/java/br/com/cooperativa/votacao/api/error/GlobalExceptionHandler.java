package br.com.cooperativa.votacao.api.error;

import br.com.cooperativa.votacao.api.ui.builder.TelaFactory;
import br.com.cooperativa.votacao.api.ui.dto.Tela;
import br.com.cooperativa.votacao.config.CorrelationIdFilter;
import br.com.cooperativa.votacao.domain.exception.NegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    /** Prefixo das rotas que devolvem telas em vez de ProblemDetail. */
    private static final String PREFIXO_TELAS = "/api/v1/telas";

    /** Base dos identificadores de tipo de erro publicados na documentacao. */
    private static final String BASE_TIPO = "https://api.cooperativa.com/erros/";

    private final TelaFactory telas;
    private final Clock clock;

    /**
     * Trata as falhas previstas pelas regras de negocio.
     *
     * @param e excecao de negocio
     * @param request requisicao que originou a falha
     * @return ProblemDetail ou tela de erro, conforme a superficie acionada
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Object> tratarNegocio(NegocioException e, HttpServletRequest request) {
        log.warn("Regra de negocio violada: [{}] {}", e.getTipo(), e.getMessage());

        if (ehTela(request)) {
            return ResponseEntity.ok(telaDeErro(e.getTitulo(), e.getMessage()));
        }

        // A traducao de natureza de falha para status HTTP vive em um unico
        // lugar; aqui apenas se aplica o resultado.
        var status = MapeadorDeStatus.de(e.getTipo());
        return ResponseEntity.status(status)
                .body(problema(status, e.getCodigo(), e.getTitulo(), e.getMessage(), request));
    }

    /**
     * Trata falhas de validacao de Bean Validation.
     *
     * @param e excecao com os campos invalidos
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
     * @param e excecao de leitura
     * @param request requisicao que originou a falha
     * @return {@code 400} com mensagem generica e segura
     */
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
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
     * @param e violacao de integridade
     * @param request requisicao que originou a falha
     * @return {@code 409} sem expor detalhes do banco
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> tratarIntegridade(
            DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn(
                "Violacao de integridade nao traduzida: {}", e.getMostSpecificCause().getMessage());
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
     * Trata rota inexistente e metodo nao suportado.
     *
     * @param e excecao de roteamento
     * @param request requisicao que originou a falha
     * @return {@code 404} para rota inexistente, {@code 405} para metodo nao suportado
     */
    @ExceptionHandler({
        NoResourceFoundException.class,
        HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<Object> tratarRoteamento(Exception e, HttpServletRequest request) {
        var metodoNaoSuportado = e instanceof HttpRequestMethodNotSupportedException;
        var status = metodoNaoSuportado ? HttpStatus.METHOD_NOT_ALLOWED : HttpStatus.NOT_FOUND;
        var detalhe =
                metodoNaoSuportado
                        ? "O metodo %s nao e suportado neste recurso."
                                .formatted(request.getMethod())
                        : "Nao existe recurso em %s.".formatted(request.getRequestURI());

        log.warn(
                "Requisicao para rota invalida: {} {}",
                request.getMethod(),
                request.getRequestURI());

        if (ehTela(request)) {
            return ResponseEntity.ok(telaDeErro("Tela nao encontrada", detalhe));
        }

        return ResponseEntity.status(status)
                .body(problema(status, "rota-invalida", "Rota invalida", detalhe, request));
    }

    /**
     * Trata violacoes de restricao em parametros de requisicao.
     *
     * @param e violacao de restricao
     * @param request requisicao que originou a falha
     * @return {@code 400} descrevendo o parametro recusado
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> tratarParametroInvalido(
            ConstraintViolationException e, HttpServletRequest request) {

        var detalhe =
                e.getConstraintViolations().stream()
                        .map(v -> "%s: %s".formatted(ultimoNo(v), v.getMessage()))
                        .collect(Collectors.joining(" "));

        log.warn("Parametro de requisicao invalido: {}", detalhe);

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
     * Extrai o nome do parametro violado do caminho da propriedade.
     *
     * @param violacao violacao de restricao
     * @return o nome do parametro
     */
    private static String ultimoNo(jakarta.validation.ConstraintViolation<?> violacao) {
        var caminho = violacao.getPropertyPath().toString();
        var ponto = caminho.lastIndexOf('.');
        return ponto >= 0 ? caminho.substring(ponto + 1) : caminho;
    }

    /**
     * Ultimo recurso para falhas inesperadas.
     *
     * @param e excecao inesperada
     * @param request requisicao que originou a falha
     * @return {@code 500} com mensagem generica
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarInesperado(Exception e, HttpServletRequest request) {
        log.error(
                "Falha inesperada ao processar {} {}",
                request.getMethod(),
                request.getRequestURI(),
                e);

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
     * @param titulo titulo da tela
     * @param mensagem explicacao ao usuario
     * @return a tela de erro
     */
    private Tela telaDeErro(String titulo, String mensagem) {
        return telas.erro(titulo, mensagem);
    }

    /**
     * Monta o corpo de erro no padrao RFC 7807.
     *
     * @param status status HTTP
     * @param codigo identificador estavel do erro
     * @param titulo titulo curto
     * @param detalhe explicacao ao consumidor
     * @param request requisicao que originou a falha
     * @return o ProblemDetail preenchido
     */
    private ProblemDetail problema(
            HttpStatus status,
            String codigo,
            String titulo,
            String detalhe,
            HttpServletRequest request) {
        var problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create(BASE_TIPO + codigo));
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
