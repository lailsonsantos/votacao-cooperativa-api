# Plano de Desenvolvimento — Assembleia de Votação Cooperativa

> Teste técnico — vaga de Desenvolvedor Sênior
> Plano de desenvolvimento **e registro da execução**. Escrito antes da primeira
> linha de código para que cada decisão pudesse ser justificada (critério
> explícito: *"Explicação breve do porquê das escolhas tomadas"*), e atualizado
> com o que foi efetivamente construído.

**No ar:** https://votacao-cooperativa-web.onrender.com
· API: https://votacao-cooperativa-api.onrender.com/api/v1
· Swagger: https://votacao-cooperativa-api.onrender.com/swagger-ui.html

**Repositórios**

| Repositório | Conteúdo | Testes |
|---|---|---|
| [`votacao-cooperativa-api`](https://github.com/lailsonsantos/votacao-cooperativa-api) | Spring Boot 3.3, Java 21, PostgreSQL, Flyway, Docker | 71 |
| [`votacao-cooperativa-web`](https://github.com/lailsonsantos/votacao-cooperativa-web) | React 18, Vite, TypeScript estrito | 14 |

---

## 0. O que foi entregue

Este documento nasceu como plano. As decisões abaixo foram tomadas depois dele e
esta seção registra o que mudou — o resto do documento segue válido como escrito.

### 0.1 Dois repositórios, não um monorepo

Públicos, para que o avaliador acesse pelo link sem precisar de convite. A
separação também deixa claro o limite do que o enunciado avalia: o backend é a
entrega, o cliente é diferencial.

### 0.2 Web responsivo, não mobile nativo

O enunciado fala em "solução para dispositivos móveis", mas também diz que *"a
aplicação cliente não faz parte da avaliação"*. O cliente foi construído como
**aplicação web responsiva**, atendendo desktop e celular a partir de uma única
base de código — decisão do autor, que não trabalha com mobile nativo.

Isso não enfraquece a demonstração do Anexo 1: o protocolo Server-Driven UI
independe da plataforma do cliente. O renderizador é exercitado dentro de uma
moldura de celular, com os mesmos payloads que um app nativo receberia.

### 0.3 Ajustes de plataforma feitos durante a implementação

| Situação encontrada | Resolução |
|---|---|
| O Docker Engine 29 elevou a versão mínima da API aceita e recusa com `400` a versão 1.32 que o docker-java negocia por padrão — os testes com Testcontainers não subiam | `api.version=1.44` fixado na configuração do Failsafe, no próprio `pom.xml`, para que a suíte rode em qualquer máquina sem configuração manual |
| O gate de cobertura lia apenas os testes unitários e reprovava em 35%, ignorando as regras de borda cobertas na integração | Os dois arquivos de execução do JaCoCo são fundidos antes da verificação |
| O fallback do Resilience4j só existe através do proxy do Spring; o teste instanciava o cliente com `new` e passaria mesmo sem a anotação | Cenário movido para um teste que sobe o contexto (`UserInfoFallbackIT`), exercitando a fiação real |
| O botão "Atualizar" da tela de resultado aponta para um endpoint de leitura, mas o cliente enviava `POST` em todo `botaoOk` | A presença de `body` passou a distinguir ação de navegação, no `FORMULARIO` como já era no `SELECAO` (§6) |

### 0.4 Deploy: de Heroku para portabilidade

O plano previa Heroku. A conta ficou inacessível — dormente havia tempo demais
para recuperar —, e a plataforma foi trocada pelo **Render**, cujo cadastro é
feito pela conta do GitHub.

Em vez de simplesmente substituir um conjunto de arquivos específicos por outro,
a troca virou oportunidade de eliminar o acoplamento. Render, Railway, Fly.io,
Neon e Supabase injetam a conexão do banco como uma URI:

```
postgresql://usuario:senha@host:5432/banco?sslmode=require
```

O Spring exige a URL em formato JDBC com as credenciais separadas. Um
`EnvironmentPostProcessor` faz a ponte antes da criação do `DataSource`,
preservando a query string — sem a qual o `sslmode=require` de bancos gerenciados
se perderia e a conexão seria recusada por um erro que não aponta para a causa.
A mesma imagem passa a subir em qualquer uma dessas plataformas sem alteração.

| Componente | Plano | Custo |
|---|---|---|
| API (serviço Docker) | `starter` | US$ 7/mês, cobrado por segundo |
| PostgreSQL | `free` | US$ 0 — expira em 30 dias |
| Frontend (site estático) | `free` | US$ 0, sem hibernação |

O plano `free` de serviço também funciona, mas hiberna após 15 min de
inatividade e leva ~1 min para voltar — tempo suficiente para um avaliador
concluir que a aplicação está fora do ar.

**Validação antes de gastar:** o caminho de produção foi exercitado localmente
antes de qualquer cobrança — imagem Docker limitada a 512 MB (o mesmo do plano
`starter`), PostgreSQL real, `DATABASE_URL` com caracteres especiais na senha,
Flyway aplicando as migrations, CORS e URLs de tela apontando para os domínios
públicos. Consumo em regime: **283 MB dos 512**.

### 0.5 Verificação executada

- `./mvnw verify` — **113 testes verdes**, incluindo 200 threads simultâneas
  votando com o mesmo CPF contra PostgreSQL real, com exatamente 1 voto
  persistido.
- Fluxo REST completo exercitado por HTTP: cadastro, sessão com default de 1
  minuto, votos, voto duplicado (`409`), CPF inválido (`400`) e apuração.
- Fluxo do Anexo 1 exercitado ponta a ponta no navegador, com a navegação
  inteiramente dirigida pelo servidor.
- CORS confirmado: origem não declarada recebe `403`; origem declarada, `200`.
- Layout verificado a 390 px e 768 px, sem rolagem horizontal.

---

## 1. Entendimento do desafio

### 1.1 O que foi pedido (obrigatório)

Uma API REST, em **Java + Spring Boot**, executável na nuvem, que gerencie sessões
de votação de assembleias cooperativas:

| # | Funcionalidade | Regra relevante |
|---|---|---|
| 1 | Cadastrar uma nova pauta | — |
| 2 | Abrir sessão de votação em uma pauta | Duração informada na chamada **ou 1 minuto por default** |
| 3 | Receber votos dos associados | Apenas `Sim`/`Não`; associado tem **id único**; **1 voto por pauta** |
| 4 | Contabilizar votos e dar o resultado | — |

Restrições declaradas no enunciado:

- Segurança **abstraída** — toda chamada é considerada autorizada.
- **Persistência obrigatória**: pautas e votos não podem se perder no restart.
- Java + Spring Boot obrigatórios; demais bibliotecas de livre escolha.
- **Domínio das URLs de callback deve ser configurável** (dica explícita do PDF).
- A aplicação será executada pelo avaliador → **zero fricção para subir**.

### 1.2 O ponto que decide o teste (e que a maioria ignora)

> *"O foco dessa avaliação é a comunicação entre o backend e o aplicativo mobile.
> Essa comunicação é feita através de mensagens no formato JSON, onde essas
> mensagens serão interpretadas pelo cliente para montar as telas onde o usuário
> vai interagir com o sistema."*

Isso **não** é um detalhe de anexo: é *Server-Driven UI*. O backend não devolve só
recursos de domínio — ele devolve **descrições de tela** que o app mobile renderiza
genericamente. O cliente não sabe o que é "pauta"; ele sabe renderizar `FORMULARIO`
e `SELECAO`.

Consequência de design: a solução tem **duas superfícies HTTP**, sobre o mesmo núcleo:

```
                      ┌──────────────────────────┐
  cliente mobile ───► │  /api/v1/telas/**  (BFF) │──┐
                      └──────────────────────────┘  │   ┌─────────────────┐
                                                    ├──►│ Camada de       │──► Banco
                      ┌──────────────────────────┐  │   │ aplicação/domínio│
  integrações   ───►  │  /api/v1/**  (REST puro) │──┘   └─────────────────┘
                      └──────────────────────────┘
```

**Por quê as duas e não só uma?**
- Só a de telas → violaria *"promover as seguintes funcionalidades através de uma
  API REST"* e tornaria a solução intestável como API.
- Só a REST → ignoraria o foco declarado da avaliação (Anexo 1).
- As duas, com a de telas sendo uma **casca fina** (só monta DTO de tela chamando o
  mesmo *service*), custa pouco código e cobre os dois critérios. **Sem regra de
  negócio duplicada.**

### 1.3 Contrato do Anexo 1 (extraído das imagens do PDF)

**Tela `FORMULARIO`** — coleção de itens + 1 ou 2 botões no rodapé:

```json
{
  "tipo": "FORMULARIO",
  "titulo": "TITULO TELA",
  "itens": [
    { "tipo": "TEXTO",        "texto": "Lorem ipsum dolor sit amet, consectetur adipiscing elit." },
    { "tipo": "INPUT_TEXTO",  "id": "idCampoTexto",     "titulo": "Campo de texto",  "valor": "Texto" },
    { "tipo": "INPUT_NUMERO", "id": "idCampoNumerico",  "titulo": "Campo numérico",  "valor": 999 },
    { "tipo": "INPUT_DATA",   "id": "idCampoData",      "titulo": "Campo data",      "valor": "01/01/2000" }
  ],
  "botaoOk": {
    "texto": "Ação 1",
    "url": "http://seudominio.com/ACAO1",
    "body": { "campo1": "valor1", "campo2": 123 }
  },
  "botaoCancelar": {
    "texto": "Cancelar",
    "url": "http://seudominio.com/"
  }
}
```

**Tela `SELECAO`** — lista de opções, cada uma com sua própria ação:

```json
{
  "tipo": "SELECAO",
  "titulo": "Lista de seleção",
  "itens": [
    { "texto": "Opção 1", "url": "http://seudominio.com/OPT1", "body": { "dadosOpcao": "campo de teste" } },
    { "texto": "Opção 2", "url": "http://seudominio.com/OPT2" },
    { "texto": "Opção 3", "url": "http://seudominio.com/OPT3" },
    { "texto": "Opção 4", "url": "http://seudominio.com/OPT4" }
  ]
}
```

**Comportamento do app** (idêntico para botões e itens de seleção): ao acionar, faz
`POST` para a `url`, com o `body` do objeto **mesclado com os valores digitados**,
indexados pelo `id` de cada input:

```
POST http://seudominio.com/ACAO1
{
  "campo1": "valor1",
  "campo2": 123,
  "idCampoTexto": "Texto",
  "idCampoNumerico": 999,
  "idCampoData": "01/01/2000"
}
```

Observações que viram requisito técnico:
- Datas nos campos vêm no formato **`dd/MM/yyyy`** (não ISO) → converter na borda.
- Cada `POST` de ação **devolve a próxima tela** → a navegação é dirigida pelo servidor.
- Todas as `url` são **absolutas** → o host precisa vir de configuração
  (`app.callback.base-url`), exatamente como a dica do PDF pede.

---

## 2. Decisões de arquitetura (e o porquê)

O enunciado avalia *"Simplicidade no design da solução (evitar over engineering)"*
lado a lado com *"Arquitetura do projeto"*. O equilíbrio buscado é: **camadas claras,
sem cerimônia inútil.**

| Decisão | Escolha | Justificativa |
|---|---|---|
| Estilo arquitetural | **Camadas** (api → application → domain → infrastructure) | Hexagonal completo com *ports/adapters* e modelos duplicados seria over engineering para 3 agregados. Camadas entregam a mesma testabilidade com metade do código. |
| Entidade JPA = modelo de domínio | **Sim**, sem modelo espelho | *Trade-off consciente*, documentado em ADR: aceito acoplamento a JPA em troca de menos código. O domínio continua com comportamento (métodos `estaAberta()`, `encerrar()`), não é anêmico. |
| Status da sessão | **Derivado**, não persistido | `ABERTA` se `now < fechamentoEm`, senão `FECHADA`. Elimina job de fechamento, elimina inconsistência de estado e elimina a classe de bug "sessão que ficou aberta porque o scheduler caiu". |
| Sessões por pauta | **No máximo uma** | O enunciado diz "abrir *uma* sessão *em uma* pauta". Reabrir votação mudaria a semântica do resultado. Segunda abertura → `409`. *(Item de dúvida a confirmar — §12.)* |
| Unicidade do voto | **Constraint no banco** `UNIQUE (sessao_id, associado_id)` | Checagem em aplicação (`select` antes de `insert`) tem *race condition* sob concorrência — e o Bônus 2 é justamente sobre concorrência. O banco é a única fonte de verdade correta. |
| Resultado da votação | **Calculado por `COUNT ... GROUP BY`**, não persistido | Nunca carrega a lista de votos em memória. Com 500 mil votos, `count` agregado é O(índice), carregar a lista é OOM. |
| Tempo | **`Instant`/UTC** em todo o backend | Evita a classe inteira de bugs de fuso e horário de verão. Formatação local só na borda. |
| `Clock` injetado | **Sim**, `@Bean Clock` | Permite testar "sessão expirou" sem `Thread.sleep`, o que mantém a suíte rápida e determinística. |
| Identificação do associado | `associadoId` = **CPF** (string, 11 dígitos) | Unifica o "id único" do requisito base com o CPF do Bônus 1, evitando dois identificadores concorrentes. *(Item de dúvida — §12.)* |

### 2.1 Estrutura de pacotes

```
br.com.cooperativa.votacao
├── VotacaoApplication.java
│
├── config/                        # Beans de infraestrutura transversal
│   ├── ClockConfig.java           #   Clock injetável (testabilidade do tempo)
│   ├── OpenApiConfig.java         #   Metadados do Swagger
│   ├── RestClientConfig.java      #   RestClient + timeouts do serviço externo
│   ├── CacheConfig.java           #   Caffeine (resultado de sessão encerrada)
│   └── CorrelationIdFilter.java   #   MDC para rastreabilidade de log
│
├── domain/                        # Regras que não dependem de framework web
│   ├── model/
│   │   ├── Pauta.java
│   │   ├── SessaoVotacao.java
│   │   ├── Voto.java
│   │   ├── OpcaoVoto.java         #   enum SIM, NAO
│   │   ├── StatusSessao.java      #   enum ABERTA, FECHADA
│   │   └── ResultadoVotacao.java  #   record (não é entidade)
│   ├── repository/                #   Interfaces Spring Data
│   └── exception/                 #   Exceções de negócio tipadas
│
├── application/                   # Casos de uso / orquestração + @Transactional
│   ├── PautaService.java
│   ├── SessaoVotacaoService.java
│   ├── VotoService.java
│   └── ResultadoService.java
│
├── infrastructure/
│   └── integration/userinfo/      # Bônus 1: cliente HTTP + DTO + fallback
│
└── api/
    ├── v1/                        # Superfície REST "pura"
    │   ├── PautaController.java
    │   ├── SessaoVotacaoController.java
    │   ├── VotoController.java
    │   └── dto/                   #   records de request/response
    ├── ui/                        # Superfície Server-Driven UI (Anexo 1)
    │   ├── TelaController.java
    │   ├── dto/                   #   Tela, ItemTela, Botao, ItemSelecao
    │   └── builder/               #   Monta cada tela a partir do domínio
    └── error/
        ├── GlobalExceptionHandler.java
        └── ApiErro.java
```

**Regra de dependência** (verificada automaticamente por ArchUnit — §9.4):
`api → application → domain`. `domain` não conhece Spring Web nem `api`.

---

## 3. Stack técnica

| Camada | Escolha | Por quê |
|---|---|---|
| Linguagem | **Java 21** (LTS) | `records`, *pattern matching*, *sealed*; LTS suportado. |
| Framework | **Spring Boot 3.3.x** | Exigido pelo enunciado. |
| Build | **Maven** + Wrapper (`./mvnw`) | Wrapper garante build reprodutível sem Maven instalado na máquina do avaliador. |
| Persistência | **Spring Data JPA + Hibernate** | Produtividade; consultas críticas escritas à mão em JPQL/SQL nativo. |
| Banco | **PostgreSQL 16** (docker) / **H2** (perfil `local`) | Postgres é o alvo real; H2 dá um caminho de execução com **zero dependência externa**, atendendo a "cuide com qualquer dependência externa". |
| Migrations | **Flyway** | Schema versionado e auditável — não `ddl-auto=update`, que é imprevisível em produção. |
| Documentação de API | **springdoc-openapi** (Swagger UI) | Critério explícito: "Documentação do código e da API". |
| Validação | **Jakarta Bean Validation** | Valida na borda; regra de negócio fica no domínio. |
| Erros | **RFC 7807 `ProblemDetail`** (nativo no Spring 6) | Padrão de mercado, sem inventar formato próprio. |
| Cliente HTTP | **`RestClient`** (Spring 6.1) | API moderna, síncrona, sem arrastar WebFlux só por causa do WebClient. |
| Resiliência | **Resilience4j** (timeout + retry + circuit breaker) | O serviço externo é instável **por design** (§7). |
| Cache | **Caffeine** | Resultado de sessão encerrada é imutável — cachear é correto e barato. |
| Logs | **SLF4J/Logback**; JSON no perfil `prod` | Critério explícito: "Logs da aplicação". |
| Observabilidade | **Spring Boot Actuator** (`/health`, `/metrics`, `/info`) | Necessário para "executar na nuvem"; custo zero de código. |
| Boilerplate | **Lombok** nas entidades; **`record`** nos DTOs | Reduz ruído sem esconder lógica. |
| Testes | JUnit 5, AssertJ, Mockito, MockMvc, **Testcontainers**, **WireMock** | §9. |
| Qualidade | **JaCoCo**, **Spotless**, **Checkstyle**, **ArchUnit**, **SpotBugs** | Critério explícito: "testes automatizados e ferramentas de qualidade". |
| Carga | **k6** (script versionado no repo) | Bônus 2 pede evidência, não só discurso. |
| Frontend | **React 18 + Vite + TypeScript** | Não é exigido pelo enunciado — entra como diferencial (§10). |

---

## 4. Modelo de domínio

```
┌───────────────────┐        0..1  ┌──────────────────────┐        *   ┌────────────────┐
│      Pauta        │──────────────│   SessaoVotacao      │────────────│      Voto      │
├───────────────────┤              ├──────────────────────┤            ├────────────────┤
│ id       UUID PK  │              │ id          UUID PK  │            │ id     UUID PK │
│ titulo   VARCHAR  │              │ pauta_id    FK UNIQUE│            │ sessao_id FK   │
│ descricao TEXT    │              │ abertura_em TIMESTAMP│            │ associado_id   │
│ criada_em TS      │              │ fechamento_em TS     │            │ opcao  VARCHAR │
└───────────────────┘              └──────────────────────┘            │ criado_em TS   │
                                                                        └────────────────┘
                                                        UNIQUE (sessao_id, associado_id)
```

### 4.1 Comportamento no domínio (não é modelo anêmico)

```java
/** Sessão de votação vinculada a uma pauta. */
public class SessaoVotacao {

    /**
     * Indica se a sessão ainda aceita votos.
     *
     * <p>O status é <strong>derivado</strong> do relógio em vez de persistido:
     * assim não existe estado a ser reconciliado por um job de fechamento, e a
     * sessão nunca fica "aberta" indevidamente após uma queda da aplicação.
     *
     * @param agora instante de referência, injetado para permitir teste determinístico
     * @return {@code true} enquanto {@code agora} for anterior ao fechamento
     */
    public boolean estaAberta(Instant agora) {
        return agora.isBefore(fechamentoEm);
    }
}
```

### 4.2 Índices

| Índice | Finalidade |
|---|---|
| `uk_voto_sessao_associado (sessao_id, associado_id)` | Garante 1 voto/associado **e** serve a checagem de duplicidade. |
| `ix_voto_sessao_opcao (sessao_id, opcao)` | Torna o `COUNT ... GROUP BY opcao` do resultado um *index-only scan*. |
| `uk_sessao_pauta (pauta_id)` | Garante a regra "uma sessão por pauta" no banco. |

### 4.3 Regras de negócio (cada uma vira um teste)

| Regra | Violação → resposta |
|---|---|
| Pauta precisa existir | `404 Not Found` |
| Sessão só abre uma vez por pauta | `409 Conflict` |
| Duração da sessão > 0 (default 1 min) | `400 Bad Request` |
| Voto só em sessão existente | `409 Conflict` |
| Voto só com sessão aberta | `422 Unprocessable Entity` |
| Um voto por associado por pauta | `409 Conflict` |
| Opção deve ser `SIM` ou `NAO` | `400 Bad Request` |
| CPF sintaticamente válido (dígitos verificadores) | `400 Bad Request` |
| CPF desconhecido no serviço externo | `422 Unprocessable Entity` |
| Associado `UNABLE_TO_VOTE` | `422 Unprocessable Entity` |
| Resultado só de pauta com sessão | `409 Conflict` |

---

## 5. API REST v1 — contrato

Base: `/api/v1`

| Método | Rota | Ação | Sucesso |
|---|---|---|---|
| `POST` | `/pautas` | Cadastra pauta | `201` + `Location` |
| `GET` | `/pautas` | Lista paginada | `200` |
| `GET` | `/pautas/{id}` | Detalha pauta | `200` |
| `POST` | `/pautas/{id}/sessao` | Abre sessão | `201` |
| `GET` | `/pautas/{id}/sessao` | Consulta sessão | `200` |
| `POST` | `/pautas/{id}/votos` | Registra voto | `201` |
| `GET` | `/pautas/{id}/resultado` | Apura resultado | `200` |

**Abrir sessão** — `POST /api/v1/pautas/{id}/sessao`

```json
{ "duracaoMinutos": 5 }
```
`duracaoMinutos` é opcional; ausente ou `null` → **1 minuto** (default do enunciado).

```json
{
  "id": "9f1c...",
  "pautaId": "3a7b...",
  "aberturaEm": "2026-08-27T14:00:00Z",
  "fechamentoEm": "2026-08-27T14:05:00Z",
  "status": "ABERTA"
}
```

**Votar** — `POST /api/v1/pautas/{id}/votos`

```json
{ "associadoId": "19839091069", "opcao": "SIM" }
```

**Resultado** — `GET /api/v1/pautas/{id}/resultado`

```json
{
  "pautaId": "3a7b...",
  "titulo": "Reforma do estatuto",
  "status": "FECHADA",
  "totalVotos": 342,
  "votosSim": 200,
  "votosNao": 142,
  "resultado": "APROVADA"
}
```

`resultado` ∈ `APROVADA` | `REPROVADA` | `EMPATE` | `SEM_VOTOS`.
Enquanto a sessão está aberta a apuração é **parcial** e vem sinalizada por
`status: "ABERTA"` — o resultado só é definitivo após o fechamento.

### 5.1 Formato de erro (RFC 7807)

```json
{
  "type": "https://api.cooperativa.com/erros/voto-duplicado",
  "title": "Voto duplicado",
  "status": 409,
  "detail": "O associado já registrou voto nesta pauta.",
  "instance": "/api/v1/pautas/3a7b.../votos",
  "correlationId": "0f3c9a12-...",
  "timestamp": "2026-08-27T14:03:11Z"
}
```

O `correlationId` fecha o ciclo com o log (§8): o avaliador copia o id da resposta
e encontra a requisição inteira no log.

---

## 6. Camada Server-Driven UI — as telas do Anexo 1

Base: `/api/v1/telas`. Toda resposta é uma tela `FORMULARIO` ou `SELECAO`.
Todo `POST` **executa a ação e devolve a próxima tela**.

### 6.1 Fluxo completo

```
GET /telas                             SELECAO   "Assembleia Cooperativa"
   ├─ "Nova pauta" ─────────────────►  GET  /telas/pautas/nova
   └─ "Pautas" ─────────────────────►  GET  /telas/pautas

GET /telas/pautas/nova                 FORMULARIO  inputs: titulo, descricao
   └─ botaoOk ──────────────────────►  POST /telas/pautas          → tela da pauta criada

GET /telas/pautas                      SELECAO   uma opção por pauta
   └─ item ─────────────────────────►  GET  /telas/pautas/{id}

GET /telas/pautas/{id}                 FORMULARIO — conteúdo depende do estado:
   ├─ sem sessão   → input duracaoMinutos (valor 1)
   │                    └─ botaoOk ──►  POST /telas/pautas/{id}/sessao   → tela de votação
   ├─ sessão ABERTA→ input cpf
   │                    └─ botaoOk ──►  POST /telas/pautas/{id}/votos/identificacao
   │                                      (valida CPF — Bônus 1)         → SELECAO Sim/Não
   └─ sessão FECHADA→ textos do resultado apurado

SELECAO "Sim / Não"
   └─ item ─────────────────────────►  POST /telas/pautas/{id}/votos    → tela de resultado
```

**Por que a votação é em dois passos (CPF → Sim/Não)?**
`FORMULARIO` só oferece `botaoOk` + `botaoCancelar`, e mapear "Não" em "Cancelar"
seria semanticamente errado — cancelar não pode registrar voto. Coletar o CPF em um
`FORMULARIO` e oferecer as opções em um `SELECAO` respeita o vocabulário do Anexo 1
e ainda permite **validar o CPF antes** de mostrar as opções, dando erro cedo.

### 6.2 Exemplos concretos de payload

`GET /api/v1/telas/pautas/{id}` — sessão ainda não aberta:

```json
{
  "tipo": "FORMULARIO",
  "titulo": "Reforma do estatuto",
  "itens": [
    { "tipo": "TEXTO", "texto": "Atualização dos artigos 12 a 18 do estatuto social." },
    { "tipo": "TEXTO", "texto": "Situação: nenhuma sessão de votação foi aberta." },
    { "tipo": "INPUT_NUMERO", "id": "duracaoMinutos", "titulo": "Duração da sessão (minutos)", "valor": 1 }
  ],
  "botaoOk": {
    "texto": "Abrir sessão",
    "url": "http://localhost:8080/api/v1/telas/pautas/3a7b.../sessao",
    "body": {}
  },
  "botaoCancelar": {
    "texto": "Voltar",
    "url": "http://localhost:8080/api/v1/telas/pautas"
  }
}
```

`POST .../votos/identificacao` com `{"cpf": "19839091069"}` → devolve:

```json
{
  "tipo": "SELECAO",
  "titulo": "Reforma do estatuto",
  "itens": [
    { "texto": "Sim", "url": "http://localhost:8080/api/v1/telas/pautas/3a7b.../votos",
      "body": { "cpf": "19839091069", "opcao": "SIM" } },
    { "texto": "Não", "url": "http://localhost:8080/api/v1/telas/pautas/3a7b.../votos",
      "body": { "cpf": "19839091069", "opcao": "NAO" } }
  ]
}
```

### 6.3 URLs configuráveis (dica explícita do PDF)

```yaml
app:
  callback:
    base-url: ${APP_CALLBACK_BASE_URL:http://localhost:8080}
```

Um único componente `UrlTelaFactory` monta todas as URLs absolutas a partir dessa
propriedade. Trocar emulador ↔ dispositivo físico ↔ nuvem = mudar **uma** variável
de ambiente, sem recompilar.

### 6.4 Erro na camada de telas

Um `409` cru quebraria a experiência do app. Erros de negócio nas rotas `/telas/**`
são convertidos pelo `GlobalExceptionHandler` em uma **tela `FORMULARIO` de erro**
(status HTTP `200`, com o texto do erro e um botão "Voltar"), enquanto as rotas
`/api/v1/**` continuam devolvendo `ProblemDetail` com o status correto. Duas
superfícies, dois contratos de erro coerentes com cada consumidor.

---

## 7. Bônus 1 — Integração com o serviço de CPF

`GET https://user-info.herokuapp.com/users/{cpf}` → `{"status":"ABLE_TO_VOTE"}` |
`{"status":"UNABLE_TO_VOTE"}` | `404`.

### 7.1 O problema real, dito na cara

**Esse endpoint está fora do ar.** A Heroku encerrou o *free tier* em novembro de
2022 e o host não responde mais. O enunciado ainda cita a URL, então a integração é
implementada **exatamente como especificada** — e cercada para que a indisponibilidade
não derrube a avaliação:

| Mecanismo | Configuração |
|---|---|
| URL externalizada | `app.user-info.base-url` |
| Liga/desliga | `app.user-info.enabled` (`false` → todo CPF válido pode votar) |
| Timeouts | conexão 2 s / leitura 3 s |
| Retry | 2 tentativas, backoff exponencial, só em erro transiente (5xx / timeout) |
| Circuit breaker | Resilience4j; abre em 50% de falha, fecha após 30 s |
| Fallback | `app.user-info.fallback-permite-voto` (default `true`) — **decisão de negócio explicitada**, não acidente |
| Stub local | Perfil `local` sobe **WireMock** respondendo o contrato do PDF |

A validação sintática de CPF (dígitos verificadores) é feita **antes** da chamada
remota: `400` sem gastar rede. O CPF é **mascarado no log** (`198******69`) — dado
pessoal sob LGPD não vai para arquivo de log.

### 7.2 Testes

WireMock cobre os quatro cenários: `ABLE_TO_VOTE`, `UNABLE_TO_VOTE`, `404`, timeout.
Nenhum teste toca a rede real — a suíte roda offline, condição para rodar em CI.

---

## 8. Bônus 2 — Performance (centenas de milhares de votos)

### 8.1 O que foi feito no código

| Técnica | Efeito |
|---|---|
| Escrita *insert-only*, unicidade delegada à constraint | 1 ida ao banco por voto, sem `SELECT` prévio e **sem race condition** |
| `DataIntegrityViolationException` → `409` | Traduz a constraint em erro de API sem lock aplicacional |
| `COUNT(*) ... GROUP BY opcao` em consulta dedicada | Apuração não carrega **nenhuma** entidade `Voto` em memória |
| Índice `(sessao_id, opcao)` | Apuração vira *index-only scan* |
| Sem `List<Voto>` mapeada em `SessaoVotacao` | Elimina a possibilidade de N+1 e de carga acidental da coleção |
| `@Transactional(readOnly = true)` nas leituras | Dispensa *dirty checking* e permite réplica de leitura |
| Cache Caffeine no resultado de sessão **encerrada** | Resultado fechado é imutável → cacheável sem risco de dado velho |
| Paginação obrigatória em `GET /pautas` | Impede resposta ilimitada |
| HikariCP dimensionado (`maximum-pool-size` configurável) | Evita o pool virar gargalo sob carga |
| Sessão HTTP *stateless* | Escala horizontal sem *sticky session* |

### 8.2 Evidência: teste de carga versionado

`perf/k6/votacao.js`, com massa de CPFs distintos:

| Cenário | Perfil | Meta |
|---|---|---|
| `carga_votos` | *ramp-up* até 500 VUs, 300 mil votos | p95 < 200 ms, erro < 0,1% |
| `apuracao_concorrente` | 50 VUs consultando resultado durante a votação | p95 < 100 ms |
| `voto_duplicado` | 100 VUs votando com o mesmo CPF | exatamente **1** sucesso |

O último cenário é o mais importante: é o que prova que a unicidade sobrevive à
concorrência real. Resultados e gráficos vão para `docs/performance.md`.

### 8.3 O que **não** foi feito, de propósito

Fila (Kafka/Rabbit) para ingestão assíncrona de votos, sharding e cache distribuído
resolveriam ordens de grandeza acima — e seriam **over engineering** para o escopo
avaliado. O documento registra o caminho de evolução, sem implementá-lo.

---

## 9. Bônus 3 — Versionamento da API

### 9.1 Estratégia adotada: versionamento por URI

`/api/v1/...`, fixado desde o primeiro commit via
`@RequestMapping("/api/v1")` em uma anotação composta `@ApiV1`.

**Por quê URI e não header/media type?**

| Estratégia | Prós | Contras | Veredito |
|---|---|---|---|
| **URI** (`/api/v1`) | Visível, cacheável, testável no browser/cURL, trivial de rotear em gateway | "Impuro" para puristas REST | **Escolhida** |
| Header (`X-API-Version`) | URI limpa | Invisível, quebra cache, difícil de depurar | Descartada |
| Media type (`Accept: application/vnd.coop.v1+json`) | Purista, granular por recurso | Alto atrito para cliente mobile; ferramental fraco | Descartada |
| Query param (`?version=1`) | Simples | Poluí cache e log; fácil de esquecer | Descartada |

O peso decisivo é o consumidor: **um app mobile**, que não atualiza junto com o
servidor. URI versionada permite manter `v1` e `v2` no ar simultaneamente enquanto a
base instalada migra — que é o problema real de versionamento de API mobile.

### 9.2 Política de evolução

1. **Mudança compatível** (campo novo opcional, endpoint novo) → **não** sobe versão.
   Cliente ignora o que não conhece; contrato preservado.
2. **Mudança incompatível** (remover/renomear campo, mudar tipo ou semântica) → **nova
   versão**. `v1` e `v2` coexistem; `application` e `domain` são compartilhados, só a
   camada `api` é duplicada — é justamente por isso que a regra de negócio **não**
   mora no controller.
3. **Depreciação anunciada por header**, conforme RFC 8594 / RFC 9745:
   ```
   Deprecation: Wed, 01 Oct 2026 00:00:00 GMT
   Sunset: Sun, 01 Mar 2027 00:00:00 GMT
   Link: </api/v2/pautas>; rel="successor-version"
   ```
4. **Janela mínima de 6 meses** entre depreciação e desligamento — prazo compatível
   com o ciclo de adoção de app nas lojas.
5. Cada versão tem seu grupo no Swagger (`springdoc.group-configs`) e seu conjunto de
   testes de contrato, que **não** são alterados após a publicação.

A camada `/telas` versiona junto (`/api/v1/telas`), mas tende a evoluir mais rápido —
é BFF, e o padrão Server-Driven UI existe justamente para mudar tela sem publicar app.

---

## 10. Qualidade: erros, logs, testes

### 10.1 Tratamento de erros

Hierarquia enxuta, um `@RestControllerAdvice` central:

```
RuntimeException
└── NegocioException                          (abstrata, carrega tipo + status)
    ├── RecursoNaoEncontradoException         → 404
    ├── SessaoJaAbertaException               → 409
    ├── VotoDuplicadoException                → 409
    ├── SessaoEncerradaException              → 422
    ├── SessaoNaoAbertaException              → 409
    └── AssociadoNaoAutorizadoException       → 422
```

Também tratados: `MethodArgumentNotValidException` (`400`, com a lista de campos
inválidos), `DataIntegrityViolationException` (`409`) e o *catch-all* `Exception`
(`500`, mensagem genérica ao cliente + *stack trace* completo apenas no log — nunca
vazar interno na resposta).

### 10.2 Logs

- **Correlation id**: filtro lê `X-Correlation-Id` (ou gera um), põe no MDC, devolve
  no header e no corpo do erro. Todo log da requisição carrega o id.
- **Formato**: legível no perfil `local`; **JSON** no perfil `prod`
  (`logstash-logback-encoder`), pronto para ELK/CloudWatch.
- **Níveis com critério**:
  - `INFO` — eventos de negócio: pauta criada, sessão aberta, voto registrado, resultado apurado.
  - `WARN` — rejeições esperadas: voto duplicado, sessão encerrada, CPF não autorizado.
  - `ERROR` — apenas o inesperado. *Voto duplicado não é `ERROR`* — é o sistema funcionando.
  - `DEBUG` — payloads e detalhes de integração.
- **LGPD**: CPF sempre mascarado por um `MaskingConverter` do Logback.

### 10.3 Pirâmide de testes

| Nível | Ferramenta | Alvo |
|---|---|---|
| Unitário | JUnit 5 + AssertJ + Mockito | Regras de domínio e services. `Clock` fixo — sem `sleep`. |
| Slice web | `@WebMvcTest` + MockMvc | Status HTTP, validação, serialização, formato de erro. |
| Slice dados | `@DataJpaTest` | Consultas de apuração e a constraint de unicidade. |
| Integração | `@SpringBootTest` + **Testcontainers** (Postgres real) | Fluxo ponta a ponta sobre o banco de verdade, não H2. |
| Integração externa | **WireMock** | Os quatro cenários do serviço de CPF. |
| Contrato de tela | MockMvc + `jsonPath` | Cada campo do Anexo 1 conferido contra os exemplos do PDF. |
| Concorrência | `ExecutorService` + `CountDownLatch` | 200 threads votando com o mesmo CPF → **1** persistido. |
| Arquitetura | **ArchUnit** | `domain` não importa Spring Web; `api` não importa `infrastructure`. |
| Carga | **k6** | §8.2. |

**Meta de cobertura**: JaCoCo com *quality gate* em ≥ 80% de linhas em `domain` e
`application` — falha o build abaixo disso. Cobertura é indicador, não objetivo:
o teste de concorrência vale mais que qualquer percentual.

### 10.4 Ferramentas de qualidade no build

`./mvnw verify` executa, e **falha o build** em: Spotless (formatação), Checkstyle
(convenções), SpotBugs (bugs estáticos), ArchUnit (camadas), JaCoCo (cobertura).
Um pipeline GitHub Actions roda o mesmo comando a cada push.

---

## 11. Padrão de documentação do código

> Requisito do projeto: **Javadoc em tudo** e comentários explicativos no código.

### 11.1 Regras de Javadoc

Toda classe, interface, enum, record e método **público ou protegido** tem Javadoc com:

- Uma frase-resumo terminada em ponto (vira o sumário do Javadoc gerado).
- Parágrafo de contexto **explicando o porquê**, quando houver decisão envolvida.
- `@param`, `@return`, `@throws` completos.
- `{@link}` para os tipos relacionados e `@see` para o requisito do enunciado.

Campos privados relevantes também recebem Javadoc, para que o modelo de dados seja
legível sem sair do arquivo.

### 11.2 Regras de comentário inline

Comentário explica **por que**, não **o que**. `// incrementa o contador` é ruído;
o comentário abaixo é informação que não está no código:

```java
/**
 * Registra o voto de um associado em uma pauta.
 *
 * <p>A unicidade do voto é garantida pela constraint
 * {@code uk_voto_sessao_associado} e não por consulta prévia: sob concorrência
 * alta (Bônus 2), um {@code SELECT} seguido de {@code INSERT} abre janela para
 * voto duplicado entre a verificação e a gravação.
 *
 * @param pautaId     identificador da pauta em votação
 * @param comando     CPF do associado e opção escolhida
 * @return o voto persistido
 * @throws RecursoNaoEncontradoException      se a pauta não existir
 * @throws SessaoNaoAbertaException           se a pauta não tiver sessão
 * @throws SessaoEncerradaException           se a sessão já tiver fechado
 * @throws VotoDuplicadoException             se o associado já votou nesta pauta
 * @throws AssociadoNaoAutorizadoException    se o serviço externo negar o CPF
 */
@Transactional
public Voto registrar(UUID pautaId, RegistrarVotoComando comando) {

    var sessao = sessaoRepository.findByPautaId(pautaId)
            .orElseThrow(() -> new SessaoNaoAbertaException(pautaId));

    // O relógio vem do Clock injetado, e não de Instant.now(), para que os testes
    // consigam simular o fim da sessão sem depender de tempo real.
    if (!sessao.estaAberta(clock.instant())) {
        throw new SessaoEncerradaException(pautaId);
    }

    associadoValidator.validarPodeVotar(comando.cpf());

    try {
        return votoRepository.save(Voto.de(sessao, comando));
    } catch (DataIntegrityViolationException e) {
        // Único caminho seguro para detectar duplicidade: a constraint do banco.
        // Traduzimos a exceção de infraestrutura em erro de negócio na fronteira.
        throw new VotoDuplicadoException(pautaId, comando.cpf(), e);
    }
}
```

O Javadoc do projeto é gerado com `./mvnw javadoc:javadoc` e publicado em
`target/site/apidocs`, com link no README.

### 11.3 Comentários no frontend

Mesma filosofia em TSX: JSDoc em cada componente e hook (propósito, props,
retorno), e comentários inline onde a lógica não é óbvia — sobretudo no renderizador
genérico de telas, que é a parte conceitualmente densa.

---

## 12. Frontend React (diferencial, fora do escopo obrigatório)

O enunciado diz textualmente: *"A aplicação cliente não faz parte da avaliação,
apenas os componentes do servidor."* O frontend entra, então, como **projeto
separado** (`frontend/`), sem interferir na execução nem na avaliação do backend —
e o README deixa isso explícito.

### 12.1 Por que vale a pena mesmo assim

O Anexo 1 descreve um protocolo de telas. Um renderizador funcionando é a **prova
executável** de que o backend cumpre esse protocolo — mais convincente que qualquer
print de Postman.

### 12.2 Duas aplicações em uma

**a) Simulador de cliente mobile** — a peça central.
Uma moldura de celular na tela. Dentro dela, um renderizador **100% genérico**: não
conhece "pauta", "voto" nem "sessão". Ele recebe JSON, decide entre `FORMULARIO` e
`SELECAO`, desenha os itens, coleta os inputs por `id`, faz o `POST` para a `url` do
botão com `{...body, ...valoresDosInputs}` e renderiza a tela que voltar.

```
src/mobile/
├── TelaRenderer.tsx        # Escolhe FORMULARIO x SELECAO pelo campo "tipo"
├── FormularioTela.tsx      # Renderiza itens + botaoOk/botaoCancelar
├── SelecaoTela.tsx         # Renderiza a lista de opções acionáveis
├── campos/
│   ├── CampoTexto.tsx      # tipo TEXTO         (leitura)
│   ├── InputTexto.tsx      # tipo INPUT_TEXTO
│   ├── InputNumero.tsx     # tipo INPUT_NUMERO
│   └── InputData.tsx       # tipo INPUT_DATA    (máscara dd/MM/yyyy do Anexo 1)
├── hooks/useTela.ts        # Navegação dirigida pelo servidor + estado dos inputs
└── types.ts                # Tipos TS espelhando exatamente o contrato do Anexo 1
```

Um `switch` sobre `item.tipo` com `default` que renderiza um aviso de "campo
desconhecido" em vez de quebrar — porque um cliente Server-Driven UI **precisa**
sobreviver a um servidor mais novo que ele.

**b) Painel administrativo** — consome a API REST `v1` diretamente (React Query +
axios): lista de pautas, criação, abertura de sessão, contador regressivo do
fechamento e painel de resultado com atualização periódica. Serve para demonstrar
que a superfície REST também é completa e utilizável.

### 12.3 Stack e testes do frontend

Vite + React 18 + TypeScript estrito, React Query (estado de servidor), axios com
`baseURL` vinda de `VITE_API_BASE_URL`, CSS Modules (sem arrastar biblioteca de UI
por causa de 5 telas). Vitest + Testing Library cobrindo o renderizador: dado o JSON
de exemplo do PDF, a tela correta é montada e o `POST` sai com o corpo correto.

---

## 13. Execução — atrito zero para o avaliador

O enunciado avisa: *"Iremos executar a aplicação para testá-la, cuide com qualquer
dependência externa"*. Três caminhos, do mais simples ao mais completo:

**1. Sem nada instalado além do JDK 21** — H2 em memória, WireMock embutido:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**2. Stack completa (Postgres + backend + frontend), só com Docker:**
```bash
docker compose up --build
# API .......... http://localhost:8080/api/v1
# Swagger ...... http://localhost:8080/swagger-ui.html
# Frontend ..... http://localhost:5173
# Health ....... http://localhost:8080/actuator/health
```

**3. Suíte completa (usa Testcontainers, exige Docker):**
```bash
./mvnw verify
```

### 13.1 Configuração externalizada

| Propriedade | Env var | Default | Para quê |
|---|---|---|---|
| `app.callback.base-url` | `APP_CALLBACK_BASE_URL` | `http://localhost:8080` | Host das URLs das telas (dica do PDF) |
| `app.sessao.duracao-padrao-minutos` | `APP_SESSAO_DURACAO_PADRAO` | `1` | Default do enunciado, sem número mágico no código |
| `app.user-info.base-url` | `APP_USER_INFO_URL` | `https://user-info.herokuapp.com` | Serviço de CPF |
| `app.user-info.enabled` | `APP_USER_INFO_ENABLED` | `true` | Desliga a integração |
| `app.user-info.fallback-permite-voto` | `APP_USER_INFO_FALLBACK` | `true` | Comportamento com o circuito aberto |

Nenhum segredo no repositório; tudo por variável de ambiente, pronto para nuvem.

### 13.2 Entregáveis de documentação

| Arquivo | Conteúdo |
|---|---|
| `README.md` | Como rodar, endpoints, **decisões e trade-offs**, dúvidas levantadas |
| `docs/adr/` | ADRs curtas (1 página) das 6 decisões estruturais |
| `docs/performance.md` | Cenários, números e gráficos do k6 |
| `docs/versionamento.md` | Resposta detalhada ao Bônus 3 |
| `docs/api.http` | Coleção de requisições prontas (roda no IntelliJ/VS Code) |
| Swagger UI | Documentação viva e navegável da API |

---

## 14. Roadmap de execução

Commits pequenos e atômicos — o enunciado avalia *"Mensagens e organização dos
commits"*. Padrão **Conventional Commits**, mensagens em português, corpo
justificando a mudança em vez de descrever o diff.

> *Nota:* a convenção pessoal `[branch] - descrição` das minhas diretrizes é
> específica dos repositórios de faturamento; para um teste técnico avaliado por
> terceiros, Conventional Commits comunica melhor por ser padrão de mercado.

| Etapa | Entrega | Commits previstos |
|---|---|---|
| **0. Fundação** | Projeto Maven, Java 21, Docker Compose, Flyway, Actuator, CI | `chore: estrutura inicial do projeto`, `chore: pipeline de build` |
| **1. Domínio** | Entidades, enums, exceções, migrations, testes unitários de regra | `feat: modelo de dominio de pauta, sessao e voto` |
| **2. Pauta** | Service + controller + DTOs + testes | `feat: cadastro e consulta de pautas` |
| **3. Sessão** | Abertura com duração/default, status derivado, testes | `feat: abertura de sessao de votacao` |
| **4. Voto** | Registro, unicidade por constraint, teste de concorrência | `feat: registro de votos com unicidade por associado` |
| **5. Resultado** | Apuração agregada + cache de sessão fechada | `feat: apuracao do resultado da votacao` |
| **6. Erros e logs** | `ProblemDetail`, advice, correlation id, mascaramento de CPF | `feat: tratamento padronizado de erros`, `feat: logs estruturados` |
| **7. Anexo 1** | Telas `FORMULARIO`/`SELECAO`, URLs configuráveis, testes de contrato | `feat: camada de telas server-driven do anexo 1` |
| **8. Bônus 1** | Cliente CPF, Resilience4j, WireMock, mascaramento | `feat: integracao com servico de validacao de cpf` |
| **9. Bônus 2** | Índices, consultas agregadas, k6, relatório | `perf: otimizacao para alto volume de votos` |
| **10. Bônus 3** | `docs/versionamento.md`, headers de depreciação, grupos no Swagger | `docs: estrategia de versionamento da api` |
| **11. Qualidade** | Spotless, Checkstyle, SpotBugs, ArchUnit, JaCoCo gate | `chore: ferramentas de qualidade no build` |
| **12. Javadoc** | Revisão de Javadoc e comentários em 100% do código | `docs: javadoc completo` |
| **13. Frontend** | Renderizador de telas + painel + testes | `feat: simulador de cliente e painel administrativo` |
| **14. Fechamento** | README final, ADRs, coleção `.http`, revisão geral | `docs: readme e decisoes de arquitetura` |

Ordem escolhida para que **a cada etapa exista software funcionando**: da etapa 5 em
diante o requisito obrigatório já está inteiro e testável; tudo depois é bônus e
polimento.

---

## 15. Dúvidas a sanar antes de codificar

> *"Não inicie o teste sem sanar todas as dúvidas"* — instrução literal do enunciado.
> Cada dúvida abaixo vem com a premissa que será adotada caso não haja resposta, e
> essa premissa fica registrada no README.

| # | Dúvida | Premissa adotada na ausência de resposta |
|---|---|---|
| 1 | O associado é identificado por CPF (Bônus 1) ou por um id próprio ("id único", requisito base)? Há cadastro de associados? | `associadoId` = CPF. Sem cadastro local; a validação delega ao serviço externo. |
| 2 | Uma pauta pode ter mais de uma sessão de votação (rodadas)? | Não. No máximo uma sessão por pauta; segunda tentativa → `409`. |
| 3 | Como classificar o **empate**? | Resultado `EMPATE`, sem aprovar nem reprovar. Zero votos → `SEM_VOTOS`. |
| 4 | Sessão aberta pode ser prorrogada ou encerrada antecipadamente? | Não. Duração é imutável após a abertura. |
| 5 | Consultar o resultado **durante** a sessão aberta é permitido? | Sim, marcado como parcial via `status: "ABERTA"`. |
| 6 | O voto pode ser alterado dentro da sessão? | Não. O enunciado diz "pode votar apenas uma vez". |
| 7 | O serviço `user-info.herokuapp.com` está fora do ar. Existe URL substituta ou devo usar stub? | Integração implementada conforme o PDF, com stub WireMock e *flag* de desligamento. |
| 8 | O Anexo 1 tem outros tipos de tela/campo além de `FORMULARIO`, `SELECAO`, `TEXTO`, `INPUT_TEXTO`, `INPUT_NUMERO`, `INPUT_DATA`? | Apenas os documentados. O renderizador degrada graciosamente diante de tipos desconhecidos. |
| 9 | As telas devem ser servidas pelo mesmo serviço ou por um BFF separado? | Mesmo serviço, em pacote e rota separados (`/api/v1/telas`). |
| 10 | Existe alvo de nuvem específico (AWS/GCP/Azure/K8s)? | Container agnóstico: imagem Docker, config por env var, Actuator para *probes*. |

---

## 16. Como este plano responde a cada critério de avaliação

| Critério do PDF | Onde é atendido |
|---|---|
| Simplicidade, evitar over engineering | §2 (camadas em vez de hexagonal), §8.3 (o que **não** foi feito) |
| Organização do código | §2.1 (pacotes), §10.4 (ArchUnit garantindo as camadas) |
| Arquitetura do projeto | §1.2 (duas superfícies, um núcleo), §2 |
| Boas práticas de programação | §2, §4.1 (domínio com comportamento), §11 |
| Possíveis bugs | §4.3 (matriz de regras), §2 (status derivado, `Clock` injetado, unicidade no banco), §10.3 (teste de concorrência) |
| Tratamento de erros e exceções | §5.1, §6.4, §10.1 |
| Explicação do porquê das escolhas | Este documento inteiro, `README.md` e `docs/adr/` |
| Testes automatizados e ferramentas de qualidade | §10.3, §10.4 |
| Limpeza do código | §10.4 (Spotless/Checkstyle falham o build) |
| Documentação do código e da API | §11 (Javadoc), Swagger, §13.2 |
| Logs da aplicação | §10.2 |
| Mensagens e organização dos commits | §14 |
| Bônus 1 / 2 / 3 | §7 / §8 / §9 |
| Foco: comunicação backend ↔ app mobile | §1.2, §1.3, §6, §12.2 |
