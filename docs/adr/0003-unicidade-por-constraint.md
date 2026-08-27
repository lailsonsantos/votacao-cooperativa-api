# ADR 0003 — Unicidade do voto garantida pelo banco

**Status:** aceita · **Data:** 2026-08-27

## Contexto

O enunciado exige que cada associado vote **uma única vez por pauta**. A Tarefa
Bônus 2 pede que a aplicação se comporte bem com centenas de milhares de votos,
ou seja, sob alta concorrência.

A implementação intuitiva consulta antes de gravar:

```java
if (votoRepository.existsBy(sessaoId, associadoId)) {
    throw new VotoDuplicadoException(...);
}
votoRepository.save(voto);
```

## Decisão

A unicidade é garantida pela constraint `uk_voto_sessao_associado (sessao_id,
associado_id)`. A aplicação grava direto e traduz
`DataIntegrityViolationException` em `VotoDuplicadoException`.

## Consequências

**A favor.**

- **Correção sob concorrência.** O `SELECT` seguido de `INSERT` abre uma janela
  entre a verificação e a gravação: duas requisições simultâneas do mesmo
  associado passam ambas pela checagem e ambas gravam. Sob a carga do Bônus 2
  isso não é hipotético — é o comportamento esperado. Só o banco resolve a
  corrida corretamente, sem lock aplicacional distribuído.
- **Metade das idas ao banco.** Um `INSERT` em vez de `SELECT` + `INSERT`. Com
  centenas de milhares de votos, isso é metade do tráfego de rede do caminho
  crítico.

**Contra.**

- A camada de aplicação precisa conhecer o nome da violação para traduzi-la. A
  tradução fica isolada em um único `catch` no `VotoService`.
- O caminho de erro é mais caro que o de sucesso (a transação é abortada). É o
  trade-off correto: o caso comum é o voto válido.

## Verificação

`VotacaoApiIT.unicidadeSobConcorrencia` dispara 200 threads simultâneas votando
com o mesmo CPF contra um PostgreSQL real e exige exatamente 1 sucesso. O cenário
`voto_duplicado` do k6 repete a verificação sob carga. Uma implementação com
`SELECT` prévio falha nesse teste de forma intermitente.
