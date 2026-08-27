# Relatório de performance — Tarefa Bônus 2

> *"Imagine que sua aplicação possa ser usada em cenários que existam centenas de
> milhares de votos. Ela deve se comportar de maneira performática nesses
> cenários."*

## O caminho crítico

Sob a carga descrita, três operações importam. Cada uma foi projetada para ter
custo independente do volume de votos já registrados:

| Operação | Custo | Por quê |
|---|---|---|
| Registrar voto | 1 `INSERT` | Sem `SELECT` prévio: a unicidade é da constraint ([ADR 0003](adr/0003-unicidade-por-constraint.md)) |
| Apurar resultado | 1 `COUNT ... GROUP BY` | *Index-only scan* em `ix_voto_sessao_opcao`; nenhuma entidade materializada ([ADR 0004](adr/0004-apuracao-agregada.md)) |
| Consultar sessão | 1 `SELECT` com `join fetch` | Sem consulta extra por carregamento tardio |

O ponto não óbvio: **nenhuma dessas operações cresce com a quantidade de votos**.
A contagem agregada é resolvida pelo índice, e a inserção não lê nada antes de
gravar.

## Decisões de implementação

| Técnica | Efeito |
|---|---|
| Escrita *insert-only* | Metade das idas ao banco por voto |
| `DataIntegrityViolationException` → `409` | Traduz a constraint sem lock aplicacional |
| Índice `(sessao_id, opcao)` | Apuração servida só pelo índice |
| Sem `List<Voto>` mapeada em `SessaoVotacao` | Torna impossível carregar a coleção por engano |
| `@Transactional(readOnly = true)` | Dispensa *dirty checking*; habilita réplica de leitura |
| Cache Caffeine (sessão **encerrada**) | Resultado fechado é imutável — cacheável sem risco de dado velho |
| Paginação obrigatória | Impede resposta ilimitada em `GET /pautas` |
| HikariCP dimensionado | Evita o pool virar o gargalo antes do banco |
| Sessão HTTP *stateless* | Escala horizontal sem *sticky session* |
| `hibernate.jdbc.batch_size=50` | Reduz *round-trips* em operações em lote |

## Evidência automatizada

### Teste de concorrência (roda em todo `./mvnw verify`)

`VotacaoApiIT.unicidadeSobConcorrencia`: 200 threads partem simultaneamente de um
`CountDownLatch` e votam com o **mesmo CPF** contra um PostgreSQL real.

**Resultado exigido:** exatamente 1 resposta `201`, todas as demais `409`, e
exatamente 1 voto no banco.

Este é o teste que vale mais que qualquer percentual de cobertura. Uma
implementação com `SELECT` antes do `INSERT` falha nele de forma intermitente —
e é exatamente esse tipo de bug que não aparece em teste sequencial.

### Teste de carga

[`perf/k6/votacao.js`](../perf/k6/votacao.js), com três cenários concorrentes:

| Cenário | Perfil | *Threshold* |
|---|---|---|
| `carga_votos` | *ramp-up* até 500 VUs por 4 min, CPFs distintos | p95 < 200 ms |
| `apuracao_concorrente` | 50 VUs lendo o resultado durante a votação | p95 < 100 ms, erro < 0,1% |
| `voto_duplicado` | 100 VUs com o **mesmo** CPF | exatamente **1** aceito |

```bash
k6 run perf/k6/votacao.js
k6 run -e BASE_URL=https://votacao-cooperativa-api.onrender.com perf/k6/votacao.js
```

Os *thresholds* fazem o k6 sair com código de erro quando violados, então o teste
serve como portão de qualidade e não apenas como observação.

**Nota sobre a geração de massa:** o script calcula dígitos verificadores válidos
para cada CPF. CPFs aleatórios seriam recusados com `400` e o teste mediria o
caminho de erro em vez do caminho de escrita — um erro fácil de cometer e difícil
de perceber no relatório.

## O que não foi feito, e por quê

| Alternativa | Ganho | Por que ficou de fora |
|---|---|---|
| Fila (Kafka/Rabbit) para ingestão assíncrona | Absorve picos muito acima do previsto | Introduz consistência eventual: o associado não saberia na hora se o voto foi aceito. Complexidade operacional desproporcional ao escopo |
| Cache distribuído (Redis) | Compartilha cache entre instâncias | O dado cacheado é pequeno e imutável; cache local resolve sem mais um serviço para operar |
| Sharding por pauta | Escala além de um único banco | Ordens de grandeza acima do cenário descrito |
| Contadores incrementais | Apuração em O(1) | Contenção de escrita na mesma linha — cria justamente o gargalo que se quer evitar |

O enunciado avalia "simplicidade no design da solução (evitar over engineering)"
lado a lado com performance. Estas alternativas estão registradas como caminho de
evolução, com o gatilho que as justificaria — não implementadas por antecipação.
