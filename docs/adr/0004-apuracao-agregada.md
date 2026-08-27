# ADR 0004 — Apuração por consulta agregada

**Status:** aceita · **Data:** 2026-08-27

## Contexto

Contabilizar votos e devolver o resultado é um dos quatro requisitos
obrigatórios. A Tarefa Bônus 2 estabelece a ordem de grandeza: centenas de
milhares de votos.

## Decisão

A apuração usa uma única consulta agregada, e a entidade `SessaoVotacao`
**não** mapeia uma coleção de votos:

```sql
SELECT opcao, COUNT(*) FROM voto WHERE sessao_id = ? GROUP BY opcao
```

O resultado não é persistido: é sempre derivado da contagem.

## Consequências

**A favor.**

- Nenhuma entidade `Voto` é materializada. Com 500 mil votos, carregar a lista
  para contar em memória esgota a heap; a contagem agregada é resolvida pelo
  índice `ix_voto_sessao_opcao` e o custo cresce com o número de opções (duas),
  não com o número de votos.
- Ausência da coleção mapeada elimina a possibilidade de N+1 e de carga
  acidental — não é possível escrever `sessao.getVotos()` porque o método não
  existe.
- Sem contagem persistida, não há um segundo lugar onde a verdade possa divergir
  do fato.

**Contra.**

- Cada consulta de resultado toca o banco. Mitigado por cache: o resultado de uma
  sessão **encerrada** é imutável e vai para o Caffeine. Resultados de sessão
  aberta nunca são cacheados, porque mudam a cada voto.

## Alternativa descartada

Manter contadores incrementais em `sessao_votacao`. Traria contenção de escrita
em uma única linha — exatamente o gargalo que o Bônus 2 pede para evitar — e
criaria a possibilidade de divergência entre o contador e os votos.
