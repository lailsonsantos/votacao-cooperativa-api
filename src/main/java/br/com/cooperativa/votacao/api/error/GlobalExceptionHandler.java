package br.com.cooperativa.votacao.api.error;

import br.com.cooperativa.votacao.api.ui.builder.TelaFactory;
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

    /** Base dos identificadores de tipo de erro publicados na documentação. */
    private static final String BASE_TIPO = "https://api.cooperativa.com/erros/";

    private final TelaFactory telas;
    private final Clock clock;

    /**
     * Trata as falhas previstas pelas regras de negócio.
     *
     * @param e exceção de negócio
     * @param request requisição que originou a falha
     * @return ProblemDetail ou tela de erro, conforme a superficie acionada
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Object> tratarNegocio(NegocioException e, HttpServletRequest request) {
        log.warn("Regra de negocio violada: [{}] {}", e.getTipo(), e.getMessage());

        if (ehTela(request)) {
            return ResponseEntity.ok(telas.erro(e.getTitulo(), e.getMessage()));
        }

        // A tradução de natureza de falha para status HTTP vive em um único
        // lugar; aqui apenas se aplica o resultado.
        var status = MapeadorDeStatus.de(e.getTipo());
        return ResponseEntity.status(status)
                .body(problema(status, e.getCodigo(), e.getTitulo(), e.getMessage(), request));
    }

    /**
     * Trata falhas de válidação de Bean Validation.
     *
     * @param e exceção com os campos inválidos
     * @param request requisição que originou a falha
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
            return ResponseEntity.ok(telas.erro("Dados invalidos", detalhe));
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
     * @param e exceção de leitura
     * @param request requisição que originou a falha
     * @return {@code 400} com mensagem genérica e segura
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
            return ResponseEntity.ok(telas.erro("Dados invalidos", detalhe));
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
     * Rede de segurança para violações de integridade não traduzidas antes.
     *
     * @param e violação de integridade
     * @param request requisição que originou a falha
     * @return {@code 409} sem expor detalhes do banco
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> tratarIntegridade(
            DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn(
                "Violacao de integridade nao traduzida: {}", e.getMostSpecificCause().getMessage());
        var detalhe = "A operacao conflita com dados ja registrados.";

        if (ehTela(request)) {
            return ResponseEntity.ok(telas.erro("Conflito", detalhe));
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
     * Trata rota inexistente e método não suportado.
     *
     * @param e exceção de roteamento
     * @param request requisição que originou a falha
     * @return {@code 404} para rota inexistente, {@code 405} para método não suportado
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
            return ResponseEntity.ok(telas.erro("Tela nao encontrada", detalhe));
        }

        return ResponseEntity.status(status)
                .body(problema(status, "rota-invalida", "Rota invalida", detalhe, request));
    }

    /**
     * Trata violações de restrição em parametros de requisição.
     *
     * @param e violação de restrição
     * @param request requisição que originou a falha
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
            return ResponseEntity.ok(telas.erro("Dados invalidos", detalhe));
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
     * @param violação violação de restrição
     * @return o nome do parametro
     */
    private static String ultimoNo(jakarta.validation.ConstraintViolation<?> violacao) {
        var caminho = violacao.getPropertyPath().toString();
        var ponto = caminho.lastIndexOf('.');
        return ponto >= 0 ? caminho.substring(ponto + 1) : caminho;
    }

    /**
     * Último recurso para falhas inesperadas.
     *
     * @param e exceção inesperada
     * @param request requisição que originou a falha
     * @return {@code 500} com mensagem genérica
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
            return ResponseEntity.ok(telas.erro("Erro inesperado", detalhe));
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
     * Indica se a requisição veio da superficie de telas.
     *
     * @param request requisição em tratamento
     * @return {@code true} se a rota pertencer a {@code /api/v1/telas}
     */
    private boolean ehTela(HttpServletRequest request) {
        return request.getRequestURI().startsWith(PREFIXO_TELAS);
    }

    /**
     * Monta a tela de erro apresentada ao cliente do Anexo 1.
     *
     * @param titulo titulo da tela
     * @param mensagem explicação ao usuário
     * @return a tela de erro
     */

    /**
     * Monta o corpo de erro no padrão RFC 7807.
     *
     * @param status status HTTP
     * @param código identificador estável do erro
     * @param titulo titulo curto
     * @param detalhe explicação ao consumidor
     * @param request requisição que originou a falha
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
        // erro consegue localizar o rastro completo da requisição no log.
        var correlationId = CorrelationIdFilter.atual();
        if (correlationId != null) {
            problema.setProperty("correlationId", correlationId);
        }

        return problema;
    }
}
