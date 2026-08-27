# ADR 0002 — Status da sessão derivado, não persistido

**Status:** aceita · **Data:** 2026-08-27

## Contexto

Uma sessão de votação fica aberta por um tempo determinado. A implementação mais
comum persiste uma coluna `status` e usa um job agendado para virá-la de
`ABERTA` para `FECHADA` no vencimento.

## Decisão

O status **não** é persistido. É derivado a cada consulta pela comparação entre o
instante atual e `fechamento_em`:

```java
public boolean estaAberta(Instant agora) {
    return agora.isBefore(fechamentoEm);
}
```

## Consequências

**A favor.**

- Elimina uma classe inteira de bug: "a sessão ficou aberta porque o agendador
  não rodou", que aparece se a aplicação cair, se o job atrasar, ou se houver
  mais de uma instância disputando a mesma execução.
- Elimina o próprio agendador e o estado a reconciliar.
- O valor derivado é sempre correto por construção. Não existe janela entre o
  vencimento real e a atualização da coluna.

**Contra.**

- Não há um evento "sessão encerrada" para reagir. Se no futuro for preciso
  publicar o resultado em uma fila no momento do fechamento, será necessário um
  agendador — mas ele passará a ser um *publicador*, não a fonte da verdade do
  status.
- Uma consulta do tipo "todas as sessões abertas" vira `WHERE fechamento_em > ?`
  em vez de `WHERE status = 'ABERTA'`. O índice resolve; não há perda prática.

## Relacionada

[ADR 0005](0005-clock-injetado.md) — a decisão só é testável porque o relógio é
injetado.
