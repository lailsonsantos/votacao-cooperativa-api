package br.com.cooperativa.votacao.api.v1;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marca um controlador como pertencente a versao 1 da API.
 *
 * <p>Concentrar o prefixo em uma anotacao composta e o que torna o versionamento por URI barato de
 * operar: quando surgir uma mudanca incompativel, cria-se {@code @ApiV2} e os controladores novos
 * convivem com os antigos sem duplicar servico nem dominio. E tambem o que garante que nenhum
 * controlador esqueca o prefixo &mdash; uma rota fora de {@code /api/v1} ficaria de fora de toda a
 * politica de depreciacao.
 *
 * <p>Estrategia completa e justificativa em {@code docs/versionamento.md}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
@RequestMapping("/api/v1")
public @interface ApiV1 {}
