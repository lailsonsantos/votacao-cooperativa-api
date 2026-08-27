# ADR 0005 — Relógio injetado

**Status:** aceita · **Data:** 2026-08-27

## Contexto

O comportamento central do sistema depende do tempo: uma sessão fica aberta por
um período e depois recusa votos. Testar isso com `Instant.now()` espalhado pelo
código exige `Thread.sleep`, o que torna a suíte lenta e intermitente.

## Decisão

`java.time.Clock` é um bean, injetado em todo componente que precisa do instante
atual. Nenhuma classe chama `Instant.now()`. Todo tempo é `Instant` em UTC.

## Consequências

**A favor.**

- Testar "a sessão expirou" custa zero milissegundo: basta construir o serviço
  com `Clock.fixed(...)` no instante desejado. `VotoServiceTest` verifica o
  comportamento no instante exato do fechamento, um caso de borda que seria
  impraticável de reproduzir com tempo real.
- UTC em todo o backend elimina a classe inteira de bugs de fuso e de horário de
  verão. A conversão para o fuso do usuário acontece só na apresentação.

**Contra.**

- Um parâmetro a mais no construtor dos serviços. Custo desprezível diante do que
  se ganha em testabilidade.

## Relacionada

[ADR 0002](0002-status-de-sessao-derivado.md) — o status derivado só é testável
de forma determinística por causa desta decisão.
