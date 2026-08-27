# Estratégia de versionamento da API — Tarefa Bônus 3

> *"Como você versionaria a API da sua aplicação? Que estratégia usar?"*

## Resposta curta

**Versionamento por URI**, com a versão fixada em uma anotação composta desde o
primeiro commit, e depreciação anunciada por cabeçalhos padronizados com janela
mínima de seis meses.

## Por que URI, e não as alternativas

| Estratégia | Prós | Contras | Veredito |
|---|---|---|---|
| **URI** — `/api/v1/pautas` | Visível na própria requisição; cacheável por padrão; testável direto no navegador e no cURL; roteável em gateway sem inspecionar cabeçalho | Considerada "impura" por quem lê REST ao pé da letra: o recurso é o mesmo, muda só a representação | **Escolhida** |
| Cabeçalho — `X-API-Version: 1` | URI permanece limpa | Invisível no log de acesso e no histórico do navegador; quebra cache HTTP (exige `Vary`); depurar exige ferramenta que permita editar cabeçalho | Descartada |
| Media type — `Accept: application/vnd.coop.v1+json` | A mais correta segundo a teoria REST; permite versionar recurso a recurso | Alto atrito para cliente mobile; ferramental fraco; versionar recurso a recurso multiplica combinações a testar | Descartada |
| Query param — `?version=1` | Trivial de implementar | Polui cache e log de acesso; fácil de omitir por engano, e o default vira uma armadilha silenciosa | Descartada |

**O peso decisivo é o consumidor.** O cliente desta API é um **aplicativo
mobile**, que não atualiza junto com o servidor: uma versão publicada na loja
continua em campo por meses. Isso torna obrigatório manter `v1` e `v2` no ar ao
mesmo tempo, e a URI é a forma mais barata de rotear as duas — inclusive em
camadas que não são a aplicação, como CDN, WAF ou API gateway.

O argumento de pureza REST perde para um fato operacional: quando um cliente
relata erro, o time precisa ver **na URL do log** qual versão ele estava usando.

## Como está implementado

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
@RequestMapping("/api/v1")
public @interface ApiV1 {}
```

Concentrar o prefixo em uma anotação composta tem duas consequências práticas:
nenhum controlador consegue esquecer o prefixo (uma rota fora de `/api/v1`
ficaria de fora de toda a política de depreciação), e criar a `v2` é adicionar
`@ApiV2` sem tocar nos controladores existentes.

A camada de telas versiona junto (`/api/v1/telas`), mas na prática evolui mais
rápido — é BFF, e o padrão Server-Driven UI existe justamente para mudar tela sem
publicar app.

## Política de evolução

### 1. Mudança compatível → **não** sobe a versão

Campo novo opcional na resposta, endpoint novo, valor novo em um enum de entrada
que já era opcional. Clientes ignoram o que não conhecem; o contrato é
preservado.

Isso exige uma disciplina do lado do cliente, que documentamos: **ignorar campos
desconhecidos**. O renderizador de telas do frontend faz exatamente isso — um
`switch` sobre `item.tipo` com `default` que exibe um aviso em vez de quebrar.

### 2. Mudança incompatível → **nova versão**

Remover ou renomear campo, mudar tipo, mudar a semântica de um valor existente,
tornar obrigatório um campo que era opcional.

`v1` e `v2` coexistem. **Apenas a camada `api` é duplicada** — `application` e
`domain` continuam compartilhados. É por isso que nenhuma regra de negócio mora
em controlador: se morasse, cada nova versão duplicaria também a regra, e as duas
cópias divergiriam.

### 3. Depreciação anunciada por cabeçalho

Conforme RFC 8594 (`Sunset`) e RFC 9745 (`Deprecation`):

```http
HTTP/1.1 200 OK
Deprecation: Wed, 01 Oct 2026 00:00:00 GMT
Sunset:      Sun, 01 Mar 2027 00:00:00 GMT
Link:        </api/v2/pautas>; rel="successor-version"
Warning:     299 - "A versao v1 sera desligada em 2027-03-01. Migre para /api/v2."
```

Cabeçalhos, e não corpo, para que o aviso alcance também os endpoints que
devolvem `204` e para que ferramentas de monitoramento possam detectá-lo sem
interpretar payload.

### 4. Janela mínima de seis meses

Entre o anúncio de depreciação e o desligamento. O prazo é ditado pelo ciclo real
de adoção de aplicativo nas lojas: publicação, aprovação, e o tempo até a base
instalada efetivamente atualizar. Menos que isso deixa usuários com o app
quebrado sem culpa própria.

### 5. Cada versão tem seu grupo no Swagger e seus testes

```yaml
springdoc:
  group-configs:
    - group: v1
      paths-to-match: /api/v1/**
    - group: v2
      paths-to-match: /api/v2/**
```

Os testes de contrato de uma versão publicada **não são alterados**. Se uma
mudança quebra um teste de contrato da `v1`, a mudança está errada — não o teste.
É essa regra que transforma a política em algo verificável pelo build, e não em
uma intenção no README.

## Monitoramento da migração

Métrica por versão via Actuator/Micrometer, com dimensão na URI. Sem isso, a
decisão de desligar a `v1` seria adivinhação; com ela, é possível ver a curva de
adoção da `v2` e adiar o desligamento se a base ainda não migrou.
