# Votação Cooperativa — API

API REST para gerenciar **sessões de votação em assembleias de cooperativas**.

No cooperativismo, cada associado tem direito a **um voto**, e as decisões são
tomadas em assembleia. Este sistema digitaliza esse processo: a diretoria
cadastra uma **pauta** (o assunto em deliberação), abre uma **sessão de votação**
com prazo determinado, os associados votam **Sim** ou **Não** — cada um uma única
vez — e ao final o sistema apura e devolve o resultado.

**Frontend (repositório separado):** https://github.com/lailsonsantos/votacao-cooperativa-web

---

## No ar

| | |
|---|---|
| **Aplicação** | https://votacao-cooperativa-web.onrender.com |
| **Swagger UI** | https://votacao-cooperativa-api.onrender.com/swagger-ui.html |
| **API REST** | https://votacao-cooperativa-api.onrender.com/api/v1 |
| **Telas do Anexo 1** | https://votacao-cooperativa-api.onrender.com/api/v1/telas |
| **Health** | https://votacao-cooperativa-api.onrender.com/actuator/health |

A aba **Simulador** da aplicação renderiza as telas do Anexo 1 direto do
servidor — é a forma mais rápida de ver o contrato funcionando.

> A API roda no plano `starter` do Render e fica sempre no ar. No plano gratuito
> ela hiberna após 15 min de inatividade e leva ~50 s para acordar na primeira
> requisição — nesse caso a demora não é erro, é a aplicação subindo.
>
> O banco usa o plano gratuito, que **expira 30 dias após a criação**.

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

O banco é publicado em **`localhost:5434`**, e não em 5432, para conviver com
outro PostgreSQL que já esteja rodando na máquina. Se 5434 também estiver
ocupada, use `DB_PORT=5436 docker compose up`.

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
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O perfil `local` usa **H2 em memória** e deixa a integração externa desligada, de
modo que a aplicação sobe sem nenhuma dependência externa. Console do H2 em
http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:votacao`, usuário `sa`, sem
senha).

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

### Opção 3 — suíte completa de testes

**Pré-requisito:** Docker (os testes de integração sobem um PostgreSQL real via
Testcontainers).

```bash
./mvnw verify
```

Executa 212 testes, o gate de cobertura, a verificação de formatação e a análise
estática.

### Comandos úteis

```bash
./mvnw test                      # só os unitários (rápido, sem Docker)
./mvnw spotless:apply            # corrige a formatação
./mvnw javadoc:javadoc           # documentação do código em target/site/apidocs
./mvnw package -DskipTests       # gera target/votacao.jar
k6 run perf/k6/votacao.js        # teste de carga (requer k6 instalado)
```

Uma coleção de requisições prontas está em [`api.http`](api.http), executável
direto no IntelliJ IDEA ou no VS Code com a extensão REST Client.

<details>
<summary><b>Rodando no IntelliJ IDEA</b></summary>

Abra o **`pom.xml`** (não a pasta) em *File → Open*.

**1. SDK do projeto: Java 21.** O código usa `records`, *pattern matching* e
blocos de texto; com JDK 17 não compila.

**2. Habilite o processamento de anotações.** *Settings → Build, Execution,
Deployment → Compiler → Annotation Processors → `Enable annotation processing`*.

Sem isso o Lombok não gera construtores, getters nem o campo `log`, e a IDE mostra
o projeto inteiro em vermelho — "cannot find symbol: log" — **enquanto o
`./mvnw verify` passa normalmente no terminal**. É o passo que mais causa
confusão.

**3. Rode.** Use a configuração de execução do Spring Boot apontando para
`VotacaoApplication` com o perfil `local` (sem dependências) ou `prod` (contra o
PostgreSQL do compose, com `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/votacao`,
usuário e senha `votacao`).

**Problemas comuns**

| Sintoma | Causa |
|---|---|
| `cannot find symbol: log`, mas o Maven compila | Processamento de anotações desabilitado (passo 2) |
| `invalid target release: 21` | SDK do projeto em versão anterior (passo 1) |
| `NoSuchMethodError` em Lombok ou google-java-format | O JBR embutido do IntelliJ é mais novo que a ferramenta. As versões do `pom.xml` já suportam JDK 25 — recarregue o projeto Maven |
| `Could not find a valid Docker environment` | Docker Desktop não está rodando |
| `FATAL: password authentication failed for user "votacao"` | Outro PostgreSQL ocupando a porta. É erro de *autenticação*, não de conexão: a aplicação achou um banco que não é o dela |
| `Port 8080 was already in use` | Outra aplicação na porta. Defina `SERVER_PORT=8081` nas variáveis de ambiente |

Qualquer erro que aconteça na IDE e não no terminal quase sempre é diferença de
JDK: o IntelliJ usa o JBR embutido e o terminal usa o `JAVA_HOME`. Para eliminar
a diferença, marque *Settings → Build Tools → Maven → Runner → `Delegate IDE
build/run actions to Maven`*.

</details>

---

## Arquitetura

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

```
src/main/java/br/com/cooperativa/votacao
├── config/           Beans transversais (Clock, CORS, correlationId, resiliência)
├── domain/           Modelo, portas e exceções — sem uma linha de Spring
│   ├── model/        Entidades e objetos de valor
│   ├── enums/        Enums do domínio, cada valor com id e descrição
│   ├── repository/   Portas de persistência, com apenas os métodos usados
│   └── exception/
├── application/      Portas de entrada: o contrato dos casos de uso
│   ├── impl/         Implementações e @Transactional
│   └── port/         Portas de saída (o que a aplicação precisa do mundo externo)
├── infrastructure/   Adaptadores: persistência JPA e serviço externo de CPF
└── api/
    ├── v1/           Superfície REST
    │   └── dto/      request/ e response/, separados
    ├── ui/           Superfície Server-Driven UI (Anexo 1)
    │   └── dto/      Telas, itens e enums/ do catálogo
    └── error/        Tratador global
```

A infraestrutura não é chamada por ninguém: ela **implementa** portas declaradas
pelas camadas internas, e o Spring faz a ligação em tempo de execução. A direção
das dependências é verificada por **8 regras de ArchUnit** que falham o build.

<details>
<summary><b>Decisões e trade-offs</b></summary>

**Camadas com inversão de dependência, sem modelo espelho.** As entidades JPA são
o modelo de domínio. Duplicá-las em modelos de persistência com mapeadores
dobraria a camada de dados para eliminar uma dependência de *anotações* —
`jakarta.persistence` é especificação declarativa, materialmente diferente de
`JpaRepository`, que traz uma API inteira e semântica transacional. É aí que o
custo/benefício se inverte, e é aí que a linha foi traçada.

O domínio, porém, **não depende de nenhuma linha de Spring**. As portas de
repositório declaram só os métodos usados; os adaptadores em
`infrastructure.persistence` estendem a porta **e** `JpaRepository`, com métodos
`default` delegando — o que entrega a inversão sem nenhuma classe adaptadora
escrita à mão.

**Status da sessão derivado do relógio, não persistido.** `ABERTA` se
`now < fechamentoEm`. Elimina o job de fechamento, o estado a reconciliar e a
classe inteira de bug "sessão que ficou aberta porque o agendador caiu".

**Unicidade do voto pela constraint do banco.** `SELECT` antes de `INSERT` tem
*race condition* — e a Tarefa Bônus 2 é justamente sobre concorrência. Duas
requisições simultâneas do mesmo associado passariam ambas pela checagem. Delegar
ao banco também elimina uma ida de rede por voto.

**Apuração por `COUNT ... GROUP BY`, nunca carregando votos.** Com 500 mil votos,
materializar a lista para contá-la esgota a heap; a contagem agregada é resolvida
pelo índice `ix_voto_sessao_opcao`.

**`Instant`/UTC com `Clock` injetado.** Permite testar "a sessão expirou" sem
`Thread.sleep`, o que mantém a suíte rápida e determinística. Elimina também a
classe inteira de bugs de fuso e horário de verão.

**HTTP fora do domínio.** As exceções declaram a *natureza* da falha
(`TipoErro`); a tradução para status vive em `MapeadorDeStatus`, na camada de
API. A mesma regra exposta por mensageria continuaria valendo e não teria status
algum.

**Validação de CPF pelo `@CPF` do Hibernate Validator**, que já vem no
`spring-boot-starter-validation`. Escrever o cálculo dos dígitos à mão seria
reimplementar, com menos testes, o que a biblioteca padrão resolve.

**Lombok** para construtores de injeção e loggers, que não carregam decisão
alguma. O `lombok.config` declara `@Value` como anotação copiável (sem o que a
injeção do `WebConfig` falharia em execução) e liga
`addLombokGeneratedAnnotation`, para que o código gerado fique fora do gate de
cobertura.

</details>

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

| Situação | Status |
|---|---|
| Pauta inexistente | `404` |
| Sessão já aberta · sem sessão · voto duplicado | `409` |
| Sessão encerrada · associado não autorizado | `422` |
| CPF inválido · paginação fora dos limites | `400` |

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

A validação dos **dígitos verificadores** acontece antes da chamada remota:
`400` sem gastar rede. O CPF é **mascarado no log** (`198******69`) — dado
pessoal sob LGPD não vai para arquivo de log, e um conversor do Logback garante a
máscara mesmo em mensagens vindas de bibliotecas de terceiros.

A camada de aplicação depende da porta `ConsultaAptidaoParaVotar`, não do cliente
HTTP: se a cooperativa passar a manter cadastro próprio de associados, basta uma
implementação nova. Os quatro cenários do contrato são cobertos com **WireMock**;
nenhum teste toca a rede real.

---

## Tarefa Bônus 2 — Performance

> *"Imagine que sua aplicação possa ser usada em cenários que existam centenas de
> milhares de votos."*

Sob essa carga, três operações importam — e nenhuma delas cresce com a quantidade
de votos já registrados:

| Operação | Custo | Por quê |
|---|---|---|
| Registrar voto | 1 `INSERT` | Sem `SELECT` prévio: a unicidade é da constraint |
| Apurar resultado | 1 `COUNT ... GROUP BY` | *Index-only scan*; nenhuma entidade materializada |
| Consultar sessão | 1 `SELECT` com `join fetch` | Sem consulta extra por carregamento tardio |

| Técnica | Efeito |
|---|---|
| Escrita *insert-only* | Metade das idas ao banco por voto |
| `DataIntegrityViolationException` → `409` | Traduz a constraint sem lock aplicacional |
| Índice `(sessao_id, opcao)` | Apuração servida só pelo índice |
| Sem `List<Voto>` mapeada em `SessaoVotacao` | Torna impossível carregar a coleção por engano |
| `@Transactional(readOnly = true)` | Dispensa *dirty checking*; habilita réplica de leitura |
| Cache Caffeine em sessão **encerrada** | Resultado fechado é imutável — cacheável sem risco |
| Paginação com teto de 100 | Impede resposta ilimitada |
| HikariCP dimensionado | Evita o pool virar gargalo antes do banco |
| `hibernate.jdbc.batch_size=50` | Reduz *round-trips* em lote |

### Evidência

**Teste de concorrência**, em todo `./mvnw verify`: 200 threads partem de um
`CountDownLatch` e votam com o **mesmo CPF** contra PostgreSQL real. Exige
exatamente 1 resposta `201`, todas as demais `409`, e 1 voto no banco. Uma
implementação com `SELECT` prévio falha nele de forma intermitente — e é
exatamente esse tipo de bug que não aparece em teste sequencial.

**Teste de carga** em [`perf/k6/votacao.js`](perf/k6/votacao.js):

| Cenário | Perfil | *Threshold* |
|---|---|---|
| `carga_votos` | *ramp-up* até 500 VUs por 4 min, CPFs distintos | p95 < 200 ms |
| `apuracao_concorrente` | 50 VUs lendo o resultado durante a votação | p95 < 100 ms |
| `voto_duplicado` | 100 VUs com o **mesmo** CPF | exatamente **1** aceito |

```bash
k6 run perf/k6/votacao.js
k6 run -e BASE_URL=https://votacao-cooperativa-api.onrender.com perf/k6/votacao.js
```

O script calcula dígitos verificadores válidos para cada CPF: com números
aleatórios a API responderia `400` e o teste mediria o caminho de erro em vez do
de escrita.

### O que não foi feito, e por quê

| Alternativa | Por que ficou de fora |
|---|---|
| Fila para ingestão assíncrona | Introduz consistência eventual: o associado não saberia na hora se o voto foi aceito |
| Cache distribuído | O dado cacheado é pequeno e imutável; cache local resolve sem mais um serviço para operar |
| Sharding por pauta | Ordens de grandeza acima do cenário descrito |
| Contadores incrementais | Contenção de escrita na mesma linha — cria justamente o gargalo que se quer evitar |

O enunciado avalia "simplicidade no design da solução" lado a lado com
performance. Estas alternativas ficam registradas como caminho de evolução, não
implementadas por antecipação.

---

## Tarefa Bônus 3 — Versionamento da API

> *"Como você versionaria a API da sua aplicação? Que estratégia usar?"*

**Versionamento por URI** (`/api/v1`), fixado desde o primeiro commit em uma
anotação composta `@ApiV1`.

| Estratégia | Prós | Contras | Veredito |
|---|---|---|---|
| **URI** — `/api/v1/pautas` | Visível na requisição; cacheável; testável no navegador e no cURL; roteável em gateway sem inspecionar cabeçalho | "Impura" para quem lê REST ao pé da letra | **Escolhida** |
| Cabeçalho — `X-API-Version: 1` | URI permanece limpa | Invisível no log e no histórico; quebra cache (exige `Vary`); difícil de depurar | Descartada |
| Media type — `Accept: …vnd.coop.v1+json` | A mais correta segundo a teoria | Alto atrito para cliente mobile; ferramental fraco; versionar recurso a recurso multiplica combinações | Descartada |
| Query param — `?version=1` | Trivial de implementar | Polui cache e log; fácil de omitir, e o default vira armadilha silenciosa | Descartada |

**O peso decisivo é o consumidor.** O cliente desta API é um **aplicativo
mobile**, que não atualiza junto com o servidor: uma versão publicada na loja
continua em campo por meses. Isso torna obrigatório manter `v1` e `v2` no ar ao
mesmo tempo, e a URI é a forma mais barata de rotear as duas — inclusive em
camadas que não são a aplicação, como CDN, WAF ou API gateway.

O argumento de pureza REST perde para um fato operacional: quando um cliente
relata erro, o time precisa ver **na URL do log** qual versão ele usava.

Concentrar o prefixo em `@ApiV1` tem duas consequências práticas: nenhum
controlador consegue esquecê-lo (uma rota fora de `/api/v1` ficaria de fora de
toda a política de depreciação), e criar a `v2` é adicionar `@ApiV2` sem tocar
nos controladores existentes.

### Política de evolução

**1. Mudança compatível → não sobe a versão.** Campo novo opcional, endpoint
novo. Clientes ignoram o que não conhecem. Isso exige uma disciplina do lado do
cliente, que documentamos: o renderizador de telas do frontend tem um `default`
que exibe um aviso em vez de quebrar diante de um tipo de campo desconhecido.

**2. Mudança incompatível → nova versão.** Remover ou renomear campo, mudar tipo
ou semântica. `v1` e `v2` coexistem, e **apenas a camada `api` é duplicada** —
`application` e `domain` continuam compartilhados. É por isso que nenhuma regra
de negócio mora em controlador: se morasse, cada versão duplicaria a regra e as
cópias divergiriam.

**3. Depreciação anunciada por cabeçalho**, conforme RFC 8594 e RFC 9745:

```http
Deprecation: Wed, 01 Oct 2026 00:00:00 GMT
Sunset:      Sun, 01 Mar 2027 00:00:00 GMT
Link:        </api/v2/pautas>; rel="successor-version"
```

Cabeçalhos, e não corpo, para que o aviso alcance também respostas `204` e para
que o monitoramento o detecte sem interpretar payload.

**4. Janela mínima de seis meses** entre anúncio e desligamento — prazo ditado
pelo ciclo real de adoção de aplicativo nas lojas. Menos que isso deixa usuários
com o app quebrado sem culpa própria.

**5. Testes de contrato de uma versão publicada não são alterados.** Se uma
mudança quebra um teste de contrato da `v1`, a mudança está errada — não o teste.
É essa regra que transforma a política em algo verificável pelo build.

**Monitoramento.** Métrica por versão via Actuator/Micrometer. Sem isso, decidir
desligar a `v1` seria adivinhação; com ela, dá para ver a curva de adoção da `v2`.

A camada `/telas` versiona junto, mas tende a evoluir mais rápido — é BFF, e o
padrão Server-Driven UI existe justamente para mudar tela sem publicar app.

---

## Qualidade

```bash
./mvnw verify
```

**Falha o build** em: testes, cobertura, formatação (Spotless com
google-java-format) e análise estática (SpotBugs). É o mesmo comando que roda no
CI — não existe um comando especial que passe onde o local falha.

**212 testes** e **100% de cobertura** — instruções, linhas, ramos, complexidade,
métodos e classes. O gate exige 95% de linhas e 90% de ramos: um gate exatamente
em 100% transforma qualquer linha nova em build vermelho antes mesmo de existir o
teste, o que empurra para escrever teste só para o número fechar.

| Nível | Ferramenta | Alvo |
|---|---|---|
| Unitário | JUnit 5 + AssertJ + Mockito | Regras de domínio e serviços, com `Clock` fixo |
| Integração | `@SpringBootTest` + Testcontainers | Fluxo ponta a ponta em PostgreSQL real |
| Contrato de tela | MockMvc + `jsonPath` | Cada campo conferido contra o Anexo 1 |
| Integração externa | WireMock | Os 4 cenários do serviço de CPF |
| Concorrência | `ExecutorService` + `CountDownLatch` | 200 threads → 1 voto |
| Arquitetura | ArchUnit | 8 regras de direção de dependências |
| Carga | k6 | Ver Tarefa Bônus 2 |

Ramo importa mais que linha: uma linha com `if` pode aparecer coberta com metade
dos caminhos nunca exercitados.

### Logs

- Legível no perfil `local`; **JSON** (`logstash-logback-encoder`) no perfil `prod`.
- `INFO` para eventos de negócio, `WARN` para rejeições esperadas, `ERROR` só
  para o inesperado. **Voto duplicado é `WARN`, não `ERROR`** — é o sistema
  funcionando.
- CPF sempre mascarado, inclusive em mensagens de bibliotecas de terceiros.
- `correlationId` em toda linha, aceito do cliente apenas no formato
  `[A-Za-z0-9_-]{1,64}` — um valor com `CR`/`LF` permitiria dividir a resposta
  HTTP e forjar linhas de log.

---

## Deploy

### Render

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
casaria. E `APP_CALLBACK_BASE_URL` importa porque as URLs das telas do Anexo 1
são **absolutas** — com o valor errado o cliente falaria com o host errado.

### Qualquer outra plataforma

A aplicação não depende do Render. Aceita a conexão do banco de duas formas:

1. **`DATABASE_URL`** no formato URI —
   `postgresql://usuario:senha@host:porta/banco` — que é como Render, Railway,
   Fly.io, Neon e Supabase a injetam. A conversão para JDBC é feita na
   inicialização por `DatabaseUrlEnvironmentPostProcessor`, preservando a query
   string (o `sslmode=require` de bancos gerenciados, em especial).
2. **`SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`** explícitas, que têm
   precedência.

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
| — | `DATABASE_URL` | — | Conexão em formato URI, convertida na inicialização |
| — | `DB_PORT` | `5434` | Porta do PostgreSQL no host, no compose |

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

## Convenção de commits

Conventional Commits, mensagens em português, corpo justificando a mudança em vez
de descrever o diff.
