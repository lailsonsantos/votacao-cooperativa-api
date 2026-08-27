# ADR 0006 — Duas superfícies HTTP sobre um núcleo

**Status:** aceita · **Data:** 2026-08-27

## Contexto

O enunciado pede duas coisas que, lidas isoladamente, parecem concorrentes:

1. *"promover as seguintes funcionalidades através de uma API REST"*;
2. *"O foco dessa avaliação é a comunicação entre o backend e o aplicativo
   mobile… mensagens JSON que serão interpretadas pelo cliente para montar as
   telas"*, detalhadas no Anexo 1.

A segunda descreve *Server-Driven UI*: o cliente não conhece o domínio, apenas
sabe renderizar `FORMULARIO` e `SELECAO`.

## Decisão

Duas superfícies HTTP sobre o mesmo núcleo de aplicação e domínio:

- `/api/v1/**` — REST orientada a recursos;
- `/api/v1/telas/**` — camada de apresentação que devolve descrições de tela.

A camada de telas é uma casca fina: monta DTOs chamando os mesmos serviços de
aplicação.

## Consequências

**A favor.**

- Atende os dois requisitos sem escolher entre eles. Só a camada de telas
  deixaria a solução intestável como API; só a REST ignoraria o foco declarado da
  avaliação.
- Nenhuma regra de negócio duplicada — é justamente por isso que a regra não pode
  morar em controlador.
- Mudar a experiência do cliente (ordem dos campos, texto, fluxo de navegação)
  não toca serviço, domínio nem banco, que é o benefício que o padrão existe para
  entregar.

**Contra.**

- Dois contratos para manter e dois conjuntos de testes.
- Dois contratos de erro: `ProblemDetail` para a REST, tela de erro com HTTP
  `200` para as telas. É intencional — o cliente do Anexo 1 renderiza telas, não
  interpreta status HTTP.

## Verificação

`TelaContratoIT` confere campo a campo o JSON produzido contra os exemplos do
Anexo 1, incluindo a ausência de campos nulos. É o teste que impede uma
renomeação de campo de quebrar o cliente silenciosamente.
