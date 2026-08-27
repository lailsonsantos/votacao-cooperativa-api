# Votação Cooperativa — API

API REST para gerenciar **sessões de votação em assembleias de cooperativas**.

No cooperativismo, cada associado tem direito a **um voto**, e as decisões são
tomadas em assembleia. Este sistema digitaliza esse processo: a diretoria
cadastra uma **pauta** (o assunto em deliberação), abre uma **sessão de votação**
com prazo determinado, os associados votam **Sim** ou **Não** — cada um uma única
vez — e ao final o sistema apura e devolve o resultado.

Solução do teste técnico descrito em [`docs/PLANO.md`](docs/PLANO.md).

**Frontend (repositório separado):** https://github.com/lailsonsantos/votacao-cooperativa-web

---

## No ar

| | |
|---|---|
| **Aplicação** | https://votacao-cooperativa-web.onrender.com |
| **Swagger UI** | https://votacao-cooperativa-api.onrender.com/swagger-ui.html |
| **API REST** | https://votacao-cooperativa-api.onrender.com/api/v1 |
| **Telas do Anexo 1** | https://votacao-cooperativa-api.onrender.com/api/v1/telas |
| **OpenAPI (JSON)** | https://votacao-cooperativa-api.onrender.com/v3/api-docs |
| **Health** | https://votacao-cooperativa-api.onrender.com/actuator/health |

A aba **Simulador** da aplicação renderiza as telas do Anexo 1 direto do
servidor — é a forma mais rápida de ver o contrato funcionando.

### Sobre a hospedagem

O serviço roda no Render, no plano `starter`, que **fica sempre no ar**.

Se você reimplantar no plano `free` (basta trocar `plan: starter` por
`plan: free` no [`render.yaml`](render.yaml)), o comportamento muda:

| | `starter` | `free` |
|---|---|---|
| Hiberna | Não | Após **15 min** sem requisições |
| Tempo para acordar | — | **~50 s** na primeira requisição |
| Custo | US$ 7/mês, cobrado por segundo | US$ 0 |

No plano `free`, a primeira chamada depois de um período ocioso simplesmente
demora — não retorna erro. Vale abrir a URL e aguardar antes de concluir que a
aplicação está fora do ar.

> O banco de dados usa o plano gratuito do Render, que **expira 30 dias após a
> criação**, com 14 dias de carência antes da exclusão dos dados. Para uso
> prolongado, troque `plan: free` por `plan: basic-256mb` no `render.yaml`.

---

## Instalação e execução

### Opção 1 — só com Docker (recomendado)

**Pré-requisito:** Docker Desktop instalado e em execução.

```bash
git clone https://github.com/lailsonsantos/votacao-cooperativa-api.git
cd votacao-cooperativa-api
docker compose up --build
```

Sobe PostgreSQL e a API juntos. O primeiro build leva alguns minutos (baixa as
dependências Maven); os seguintes usam cache.

| Recurso | URL |
|---|---|
| API REST | http://localhost:8080/api/v1 |
| Telas (Anexo 1) | http://localhost:8080/api/v1/telas |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

Para parar: `Ctrl+C`, depois `docker compose down -v` (o `-v` apaga o volume do
banco).

### Opção 2 — sem Docker, apenas com o JDK 21

**Pré-requisito:** JDK 21. Não precisa de Maven — o projeto traz o wrapper.

```bash
git clone https://github.com/lailsonsantos/votacao-cooperativa-api.git
cd votacao-cooperativa-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O perfil `local` usa **H2 em memória** e deixa a integração externa desligada, de
modo que a aplicação sobe sem nenhuma dependência externa. Console do H2 em
http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:votacao`, usuário `sa`,
sem senha).

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

### Opção 3 — suíte completa de testes

**Pré-requisito:** Docker (os testes de integração sobem um PostgreSQL real via
Testcontainers).

```bash
./mvnw verify
```

Executa 75 testes e o gate de cobertura. Relatório em
`target/site/jacoco/index.html`.

> **Docker Engine 29+** elevou a versão mínima da API aceita. O projeto já fixa
> `api.version=1.44` na configuração do Failsafe, então nenhum ajuste é
> necessário na sua máquina.

### Comandos úteis

```bash
./mvnw test                      # apenas os testes unitários (rápido, sem Docker)
./mvnw javadoc:javadoc           # documentação do código em target/site/apidocs
./mvnw package -DskipTests       # gera target/votacao.jar
k6 run perf/k6/votacao.js        # teste de carga (requer k6 instalado)
```

Uma coleção de requisições prontas está em [`docs/api.http`](docs/api.http),
executável direto no IntelliJ IDEA ou no VS Code com a extensão REST Client.

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

Documentação navegável e testável no
[Swagger UI](https://votacao-cooperativa-api.onrender.com/swagger-ui.html).

| Método | Rota | Ação |
|---|---|---|
| `POST` | `/api/v1/pautas` | Cadastra pauta |
| `GET` | `/api/v1/pautas` | Lista paginada |
| `GET` | `/api/v1/pautas/{id}` | Detalha pauta |
| `POST` | `/api/v1/pautas/{id}/sessao` | Abre sessão (default 1 min) |
| `GET` | `/api/v1/pautas/{id}/sessao` | Consulta sessão |
| `POST` | `/api/v1/pautas/{id}/votos` | Registra voto |
| `GET` | `/api/v1/pautas/{id}/resultado` | Apura resultado |

### Exemplo rápido

```bash
API=https://votacao-cooperativa-api.onrender.com/api/v1

# 1. Cadastrar a pauta
PAUTA=$(curl -s -X POST $API/pautas -H 'Content-Type: application/json' \
  -d '{"titulo":"Reforma do estatuto","descricao":"Artigos 12 a 18."}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")

# 2. Abrir a sessão sem corpo -> aplica o default de 1 minuto
curl -s -X POST $API/pautas/$PAUTA/sessao

# 3. Votar (CPF válido é obrigatório: os dígitos verificadores são conferidos)
curl -s -X POST $API/pautas/$PAUTA/votos -H 'Content-Type: application/json' \
  -d '{"associadoId":"19839091069","opcao":"SIM"}'

# 4. Apurar
curl -s $API/pautas/$PAUTA/resultado
```

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
GET  /telas                                 → SELECAO  menu
GET  /telas/pautas                          → SELECAO  lista de pautas
GET  /telas/pautas/nova                     → FORMULARIO  cadastro
POST /telas/pautas                          → tela da pauta criada
GET  /telas/pautas/{id}                     → varia conforme o estado da sessão
POST /telas/pautas/{id}/sessao              → tela de identificação
POST /telas/pautas/{id}/votos/identificacao → SELECAO  Sim / Não
POST /telas/pautas/{id}/votos               → tela de resultado
```

Veja funcionando: https://votacao-cooperativa-api.onrender.com/api/v1/telas

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
| Arquitetura | Camadas com inversão de dependência | [ADR 0007](docs/adr/0007-inversao-de-dependencia-sem-modelo-espelho.md). O domínio declara portas; a infraestrutura as implementa. `domain` não depende de nenhuma linha de Spring — verificado por ArchUnit. |
| Entidade JPA = domínio | Sim, sem modelo espelho | [ADR 0001](docs/adr/0001-entidade-jpa-como-modelo-de-dominio.md). O domínio tem comportamento (`estaAberta()`), não é anêmico. |
| Status da sessão | Derivado do relógio | [ADR 0002](docs/adr/0002-status-de-sessao-derivado.md). Sem job de fechamento, sem estado a reconciliar. |
| Unicidade do voto | Constraint no banco | [ADR 0003](docs/adr/0003-unicidade-por-constraint.md). `SELECT` + `INSERT` tem race condition — e o Bônus 2 é sobre concorrência. |
| Apuração | `COUNT ... GROUP BY` | [ADR 0004](docs/adr/0004-apuracao-agregada.md). Nunca carrega votos em memória. |
| Tempo | `Instant`/UTC + `Clock` injetado | [ADR 0005](docs/adr/0005-clock-injetado.md). Testar "sessão expirou" sem `Thread.sleep`. |
| Duas superfícies HTTP | REST + telas sobre o mesmo núcleo | [ADR 0006](docs/adr/0006-duas-superficies-http.md). |
| Validação de CPF | `@CPF` do Hibernate Validator | Já vem no `spring-boot-starter-validation`. Escrever o cálculo dos dígitos à mão seria reimplementar, com menos testes, o que a biblioteca padrão resolve. |
| Boilerplate | Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@Getter`) | Construtores de injeção e declarações de logger não carregam decisão nenhuma. `lombok.addLombokGeneratedAnnotation` mantém o código gerado fora do gate de cobertura. |

---

## Tarefa Bônus 1 — Verificação de CPF

> ⚠️ **O endpoint `https://user-info.herokuapp.com/users/{cpf}` está fora do ar.**
> A Heroku encerrou o *free tier* em novembro de 2022 e o host não responde mais.

A integração foi implementada **exatamente como especificada** e cercada para que
a indisponibilidade de um terceiro não impeça a avaliação:

| Mecanismo | Configuração |
|---|---|
| URL externalizada | `APP_USER_INFO_URL` |
| Liga/desliga | `APP_USER_INFO_ENABLED` (padrão `false` no compose e em produção) |
| Timeouts | conexão 2 s / leitura 3 s |
| Retry | 2 tentativas, backoff exponencial, só em falha transiente |
| Circuit breaker | Resilience4j — abre em 50% de falha, fecha após 30 s |
| Fallback | `APP_USER_INFO_FALLBACK` (padrão `true`) — decisão de negócio explícita |

A validação dos **dígitos verificadores do CPF** acontece antes da chamada
remota: `400` sem gastar rede. O CPF é **mascarado no log** (`198******69`) —
dado pessoal sob LGPD não vai para arquivo de log, e um conversor do Logback
garante a máscara mesmo em mensagens vindas de bibliotecas de terceiros.

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

**Evidência:** `VotacaoApiIT.unicidadeSobConcorrencia` dispara 200 threads
votando com o mesmo CPF contra PostgreSQL real e exige exatamente **1** voto
persistido. Roda em todo `./mvnw verify`. Detalhes em
[`docs/performance.md`](docs/performance.md).

---

## Tarefa Bônus 3 — Versionamento

Versionamento **por URI** (`/api/v1`), concentrado na anotação composta `@ApiV1`.
Análise completa das alternativas e da política de depreciação:
[`docs/versionamento.md`](docs/versionamento.md).

---

## Qualidade

**75 testes**, gate de cobertura em 80% de linhas (falha o build abaixo disso).

| Nível | Ferramenta | Alvo |
|---|---|---|
| Unitário | JUnit 5 + AssertJ + Mockito | Regras de domínio e serviços, com `Clock` fixo |
| Integração | `@SpringBootTest` + Testcontainers | Fluxo ponta a ponta em PostgreSQL real |
| Contrato de tela | MockMvc + `jsonPath` | Cada campo conferido contra o Anexo 1 |
| Integração externa | WireMock | Os 4 cenários do serviço de CPF |
| Concorrência | `ExecutorService` + `CountDownLatch` | 200 threads → 1 voto |
| Arquitetura | ArchUnit | 7 regras: direção das dependências, domínio livre de framework, ausência de ciclos |

### Logs

- Legível no perfil `local`; **JSON** (`logstash-logback-encoder`) no perfil `prod`.
- `INFO` para eventos de negócio, `WARN` para rejeições esperadas, `ERROR` só
  para o inesperado. **Voto duplicado é `WARN`, não `ERROR`** — é o sistema
  funcionando.
- CPF sempre mascarado.

---

## Deploy

### Render (configurado)

1. Entre em https://render.com com a conta do GitHub.
2. **Blueprints → New Blueprint Instance** → selecione o repositório.
3. O Render lê o [`render.yaml`](render.yaml), mostra o custo e cria o serviço e
   o banco.
4. Depois do primeiro deploy, preencha no painel:

| Variável | Valor |
|---|---|
| `APP_CALLBACK_BASE_URL` | a URL pública desta API |
| `APP_CORS_ALLOWED_ORIGINS` | a URL pública do frontend |

Ambas **sem barra no final**. No `APP_CORS_ALLOWED_ORIGINS` isso é decisivo: o
navegador envia o header `Origin` sem barra, e um valor com `/` no fim nunca
casaria. E `APP_CALLBACK_BASE_URL` importa mais do que parece — as URLs das telas
do Anexo 1 são **absolutas**, então com o valor errado o cliente falaria com o
host errado.

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

---

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
| 10 | Alvo de nuvem específico? | Container agnóstico; publicado no Render |

---

## Estrutura

```
src/main/java/br/com/cooperativa/votacao
├── config/           Beans transversais (Clock, CORS, correlationId, resiliência)
├── domain/           Modelo, repositórios e exceções — sem dependência de web
│   ├── model/
│   ├── repository/   Portas de persistência, com apenas os métodos usados
│   └── exception/
├── application/      Casos de uso e @Transactional
│   └── port/         Portas de saída (o que a aplicação precisa do mundo externo)
├── infrastructure/   Adaptadores: persistência JPA e serviço externo de CPF
└── api/
    ├── v1/           Superfície REST
    ├── ui/           Superfície Server-Driven UI (Anexo 1)
    └── error/        Tratador global
```

Regra de dependência `api → application → domain`, verificada por ArchUnit. A
infraestrutura não é chamada por ninguém: ela **implementa** portas declaradas
pelas camadas internas, e o Spring faz a ligação em tempo de execução.

---

## Convenção de commits

Conventional Commits, mensagens em português, corpo justificando a mudança em vez
de descrever o diff.
