# Votação Cooperativa — API

API REST para gerenciar pautas, sessões de votação e apuração de resultados em
assembleias cooperativas. Solução do teste técnico descrito em
[`docs/PLANO.md`](docs/PLANO.md).

**Frontend (repositório separado):** https://github.com/lailsonsantos/votacao-cooperativa-web

---

## Como executar

### Opção 1 — só com Docker (recomendado para avaliação)

```bash
docker compose up --build
```

| Recurso | URL |
|---|---|
| API REST | http://localhost:8080/api/v1 |
| Telas (Anexo 1) | http://localhost:8080/api/v1/telas |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

### Opção 2 — sem Docker, apenas com JDK 21

O perfil `local` usa **H2 em memória** e deixa a integração externa desligada, de
modo que a aplicação sobe sem nenhuma dependência externa.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Opção 3 — suíte completa de testes

Requer Docker: os testes de integração sobem um PostgreSQL real via Testcontainers.

```bash
./mvnw verify
```

> **Docker Engine 29+** elevou a versão mínima da API aceita. O projeto já fixa
> `api.version=1.44` na configuração do Failsafe, então nenhum ajuste é
> necessário na sua máquina.

---

## O que foi construído

O enunciado destaca em negrito que **o foco da avaliação é a comunicação entre o
backend e o aplicativo mobile**, feita por mensagens JSON que o cliente
interpreta para montar telas. Isso é *Server-Driven UI*, e não um detalhe de
anexo. A solução tem, por isso, **duas superfícies HTTP sobre um único núcleo**:

```
                      ┌──────────────────────────┐
  cliente        ───► │  /api/v1/telas/**  (BFF) │──┐
                      └──────────────────────────┘  │   ┌──────────────────┐
                                                    ├──►│ application +    │──► PostgreSQL
                      ┌──────────────────────────┐  │   │ domain           │
  integrações    ───► │  /api/v1/**  (REST puro) │──┘   └──────────────────┘
                      └──────────────────────────┘
```

A camada de telas é uma **casca fina**: monta o DTO de tela chamando os mesmos
serviços de aplicação. **Nenhuma regra de negócio é duplicada.**

---

## API REST v1

| Método | Rota | Ação |
|---|---|---|
| `POST` | `/api/v1/pautas` | Cadastra pauta |
| `GET` | `/api/v1/pautas` | Lista paginada |
| `GET` | `/api/v1/pautas/{id}` | Detalha pauta |
| `POST` | `/api/v1/pautas/{id}/sessao` | Abre sessão (default 1 min) |
| `GET` | `/api/v1/pautas/{id}/sessao` | Consulta sessão |
| `POST` | `/api/v1/pautas/{id}/votos` | Registra voto |
| `GET` | `/api/v1/pautas/{id}/resultado` | Apura resultado |

Exemplos prontos para executar: [`docs/api.http`](docs/api.http).

### Erros — RFC 7807

```json
{
  "type": "https://api.cooperativa.com/erros/voto-duplicado",
  "title": "Voto duplicado",
  "status": 409,
  "detail": "O associado 198******69 ja registrou voto na pauta 3a7b...",
  "instance": "/api/v1/pautas/3a7b.../votos",
  "correlationId": "0f3c9a12-...",
  "timestamp": "2026-08-27T14:03:11Z"
}
```

O `correlationId` volta também no header `X-Correlation-Id` e aparece em toda
linha de log da requisição — é ele que liga o erro visto pelo usuário ao rastro
completo no log.

---

## Telas do Anexo 1

Toda resposta de `/api/v1/telas/**` é uma tela `FORMULARIO` ou `SELECAO`, e todo
`POST` **executa a ação e devolve a próxima tela**.

```
GET  /telas                              → SELECAO  menu
GET  /telas/pautas                       → SELECAO  lista de pautas
GET  /telas/pautas/nova                  → FORMULARIO  cadastro
POST /telas/pautas                       → tela da pauta criada
GET  /telas/pautas/{id}                  → varia conforme o estado da sessão
POST /telas/pautas/{id}/sessao           → tela de identificação
POST /telas/pautas/{id}/votos/identificacao → SELECAO  Sim / Não
POST /telas/pautas/{id}/votos            → tela de resultado
```

**Por que a votação tem dois passos (CPF → Sim/Não)?** `FORMULARIO` oferece
apenas `botaoOk` e `botaoCancelar`, e mapear "Não" em "Cancelar" seria
semanticamente errado — cancelar não pode registrar voto. Coletar o CPF em um
`FORMULARIO` e oferecer as opções em um `SELECAO` respeita o vocabulário do
Anexo 1 e ainda permite validar o CPF **antes** de mostrar as opções.

### URLs configuráveis

Atendendo à dica explícita do enunciado, o domínio das URLs de callback vem de
configuração:

```bash
APP_CALLBACK_BASE_URL=http://192.168.0.10:8080 docker compose up
```

Um único componente (`UrlTelaFactory`) monta todas as URLs absolutas. Trocar
emulador ↔ dispositivo físico ↔ nuvem é mudar **uma** variável de ambiente.

### Erro na camada de telas

Um `409` cru deixaria o cliente sem nada para renderizar. Erros de negócio em
`/telas/**` viram uma **tela `FORMULARIO` de erro** com HTTP `200`, enquanto
`/api/v1/**` continua devolvendo `ProblemDetail` com o status correto.

---

## Decisões e trade-offs

| Decisão | Escolha | Justificativa |
|---|---|---|
| Arquitetura | Camadas (`api → application → domain`) | Hexagonal completo com *ports/adapters* seria over engineering para 3 agregados. Verificada por ArchUnit. |
| Entidade JPA = domínio | Sim, sem modelo espelho | [ADR 0001](docs/adr/0001-entidade-jpa-como-modelo-de-dominio.md). O domínio tem comportamento (`estaAberta()`), não é anêmico. |
| Status da sessão | Derivado do relógio | [ADR 0002](docs/adr/0002-status-de-sessao-derivado.md). Sem job de fechamento, sem estado a reconciliar, sem "sessão aberta porque o scheduler caiu". |
| Unicidade do voto | Constraint no banco | [ADR 0003](docs/adr/0003-unicidade-por-constraint.md). `SELECT` + `INSERT` tem race condition — e o Bônus 2 é justamente sobre concorrência. |
| Apuração | `COUNT ... GROUP BY` | [ADR 0004](docs/adr/0004-apuracao-agregada.md). Nunca carrega votos em memória. |
| Tempo | `Instant`/UTC + `Clock` injetado | [ADR 0005](docs/adr/0005-clock-injetado.md). Testar "sessão expirou" sem `Thread.sleep`. |
| Duas superfícies HTTP | REST + telas sobre o mesmo núcleo | [ADR 0006](docs/adr/0006-duas-superficies-http.md). |

---

## Tarefa Bônus 1 — Verificação de CPF

> ⚠️ **O endpoint `https://user-info.herokuapp.com/users/{cpf}` está fora do ar.**
> A Heroku encerrou o *free tier* em novembro de 2022 e o host não responde mais.

A integração foi implementada **exatamente como especificada** e cercada para que
a indisponibilidade de um terceiro não impeça a avaliação:

| Mecanismo | Configuração |
|---|---|
| URL externalizada | `APP_USER_INFO_URL` |
| Liga/desliga | `APP_USER_INFO_ENABLED` (padrão `false` no compose) |
| Timeouts | conexão 2 s / leitura 3 s |
| Retry | 2 tentativas, backoff exponencial, só em falha transiente |
| Circuit breaker | Resilience4j — abre em 50% de falha, fecha após 30 s |
| Fallback | `APP_USER_INFO_FALLBACK` (padrão `true`) — decisão de negócio explícita |

A validação dos **dígitos verificadores do CPF** acontece antes da chamada
remota: `400` sem gastar rede. O CPF é **mascarado no log** (`198******69`) —
dado pessoal sob LGPD não vai para arquivo de log, e um conversor do Logback
garante a máscara mesmo em mensagens vindas de bibliotecas de terceiros.

Os quatro cenários do contrato são cobertos por testes com **WireMock**; nenhum
teste toca a rede real.

---

## Tarefa Bônus 2 — Performance

| Técnica | Efeito |
|---|---|
| Escrita *insert-only*, unicidade na constraint | 1 ida ao banco por voto, sem race condition |
| `COUNT(*) ... GROUP BY opcao` | Apuração não carrega nenhuma entidade `Voto` |
| Índice `(sessao_id, opcao)` | Apuração vira *index-only scan* |
| Sem `List<Voto>` mapeada | Elimina N+1 e carga acidental da coleção |
| `@Transactional(readOnly = true)` | Sem *dirty checking*; permite réplica de leitura |
| Cache Caffeine em sessão **encerrada** | Resultado fechado é imutável → cacheável sem risco |
| Paginação obrigatória | Impede resposta ilimitada |
| HikariCP dimensionado | Evita o pool virar gargalo |

### Evidência

- **Teste de concorrência automatizado**: `VotacaoApiIT.unicidadeSobConcorrencia`
  — 200 threads votando com o mesmo CPF contra PostgreSQL real; exatamente **1**
  voto persistido. Roda em todo `./mvnw verify`.
- **Teste de carga**: [`perf/k6/votacao.js`](perf/k6/votacao.js), com três
  cenários e *thresholds* que falham a execução.

```bash
k6 run perf/k6/votacao.js
```

**O que não foi feito, de propósito:** fila para ingestão assíncrona, sharding e
cache distribuído resolveriam ordens de grandeza acima — e seriam over
engineering para o escopo avaliado.

---

## Tarefa Bônus 3 — Versionamento

Versionamento **por URI** (`/api/v1`), concentrado na anotação composta `@ApiV1`.
Análise completa das alternativas e da política de depreciação:
[`docs/versionamento.md`](docs/versionamento.md).

---

## Qualidade

```bash
./mvnw verify          # testes + gate de cobertura
./mvnw javadoc:javadoc # documentação do código em target/site/apidocs
```

| Nível | Ferramenta | Alvo |
|---|---|---|
| Unitário | JUnit 5 + AssertJ + Mockito | Regras de domínio e serviços, com `Clock` fixo |
| Integração | `@SpringBootTest` + Testcontainers | Fluxo ponta a ponta em PostgreSQL real |
| Contrato de tela | MockMvc + `jsonPath` | Cada campo conferido contra o Anexo 1 |
| Integração externa | WireMock | Os 4 cenários do serviço de CPF |
| Concorrência | `ExecutorService` + `CountDownLatch` | 200 threads → 1 voto |
| Arquitetura | ArchUnit | Direção das dependências entre camadas |

**71 testes**, gate de cobertura em 80% de linhas (falha o build abaixo disso).

### Logs

- Legível no perfil `local`; **JSON** (`logstash-logback-encoder`) no perfil `prod`.
- `INFO` para eventos de negócio, `WARN` para rejeições esperadas, `ERROR` só
  para o inesperado. **Voto duplicado é `WARN`, não `ERROR`** — é o sistema
  funcionando.
- CPF sempre mascarado.

---

## Deploy

### Render (configurado)

Ambos os repositórios trazem um `render.yaml`. Para publicar:

1. Entre em https://render.com com a conta do GitHub.
2. **Blueprints → New Blueprint Instance** → selecione `votacao-cooperativa-api`.
3. O Render lê o `render.yaml`, mostra o custo e cria o serviço e o banco.
4. Depois do primeiro deploy, preencha no painel:

| Variável | Valor |
|---|---|
| `APP_CALLBACK_BASE_URL` | a URL pública desta API |
| `APP_CORS_ALLOWED_ORIGINS` | a URL pública do frontend |

`APP_CALLBACK_BASE_URL` importa mais do que parece: as URLs das telas do Anexo 1
são **absolutas**, e com o valor errado o cliente tentaria falar com o host
errado.

**Custo:** serviço `starter` US$ 7/mês (cobrado por segundo) + banco no plano
gratuito. O plano `free` de serviço também funciona, mas hiberna após 15 min de
inatividade e leva cerca de um minuto para voltar — tempo suficiente para um
avaliador concluir que a aplicação está fora do ar.

> O plano gratuito de banco do Render **expira 30 dias após a criação**, com 14
> dias de carência antes da exclusão dos dados. É suficiente para uma avaliação;
> para uso prolongado, troque `plan: free` por `plan: basic-256mb` no
> `render.yaml`.

### Qualquer outra plataforma

A aplicação não depende do Render. Ela aceita a conexão do banco de duas formas:

1. **`DATABASE_URL`** no formato URI — `postgresql://usuario:senha@host:porta/banco` —
   que é como Render, Railway, Fly.io, Neon e Supabase a injetam. A conversão
   para JDBC é feita em tempo de inicialização por
   `DatabaseUrlEnvironmentPostProcessor`, preservando a query string (o
   `sslmode=require` de bancos gerenciados, em especial).
2. **`SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`** explícitas, que têm
   precedência sobre a anterior.

Fora isso, basta a imagem Docker, a porta em `$PORT` e `/actuator/health` para as
*probes*.

## Configuração

| Propriedade | Env var | Default | Para quê |
|---|---|---|---|
| `app.callback.base-url` | `APP_CALLBACK_BASE_URL` | `http://localhost:8080` | Host das URLs das telas |
| `app.sessao.duracao-padrao-minutos` | `APP_SESSAO_DURACAO_PADRAO` | `1` | Default do enunciado |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,...` | Origens do frontend |
| `app.user-info.base-url` | `APP_USER_INFO_URL` | `https://user-info.herokuapp.com` | Serviço de CPF |
| `app.user-info.enabled` | `APP_USER_INFO_ENABLED` | `true` | Desliga a integração |
| `app.user-info.fallback-permite-voto` | `APP_USER_INFO_FALLBACK` | `true` | Comportamento com circuito aberto |
| — | `DATABASE_URL` | — | Conexão em formato URI, convertida para JDBC na inicialização |

Nenhum segredo no repositório.

---

## Premissas adotadas

O enunciado instrui: *"Não inicie o teste sem sanar todas as dúvidas."* Na
ausência de um canal para perguntar, cada dúvida foi resolvida por uma premissa
explícita — todas revisáveis:

| # | Dúvida | Premissa |
|---|---|---|
| 1 | Associado é identificado por CPF ou id próprio? | `associadoId` = CPF, unificando o "id único" do requisito base com o CPF do Bônus 1 |
| 2 | Uma pauta pode ter várias sessões? | Não. Segunda abertura → `409` |
| 3 | Como classificar empate? | `EMPATE`; zero votos → `SEM_VOTOS` |
| 4 | Sessão pode ser prorrogada? | Não. Duração imutável após a abertura |
| 5 | Consultar resultado com sessão aberta? | Sim, marcado com `parcial: true` |
| 6 | O voto pode ser alterado? | Não — "pode votar apenas uma vez" |
| 7 | O serviço de CPF está fora do ar | Implementado conforme o PDF, com flag de desligamento e stub WireMock |
| 8 | Há outros tipos de tela/campo? | Apenas os documentados no Anexo 1 |
| 9 | Telas no mesmo serviço ou BFF separado? | Mesmo serviço, pacote e rota separados |
| 10 | Alvo de nuvem específico? | Container agnóstico + Heroku e Render configurados |

---

## Estrutura

```
src/main/java/br/com/cooperativa/votacao
├── config/           Beans transversais (Clock, CORS, correlationId, resiliência)
├── domain/           Modelo, repositórios e exceções — sem dependência de web
│   ├── model/
│   ├── repository/
│   └── exception/
├── application/      Casos de uso e @Transactional
├── infrastructure/   Integração com o serviço externo de CPF
└── api/
    ├── v1/           Superfície REST
    ├── ui/           Superfície Server-Driven UI (Anexo 1)
    └── error/        Tratador global
```

Regra de dependência `api → application → domain`, verificada por ArchUnit.

---

## Convenção de commits

Conventional Commits, mensagens em português, corpo justificando a mudança em vez
de descrever o diff.
