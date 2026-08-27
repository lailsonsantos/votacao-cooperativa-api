# ADR 0001 — Entidade JPA como modelo de domínio

**Status:** aceita · **Data:** 2026-08-27

## Contexto

Uma arquitetura hexagonal ortodoxa mantém o modelo de domínio livre de qualquer
framework e espelha cada agregado em uma entidade de persistência separada, com
mapeadores entre as duas. O enunciado avalia "arquitetura do projeto" e, na mesma
lista, "simplicidade no design da solução (evitar over engineering)".

## Decisão

As entidades JPA **são** o modelo de domínio. Não há modelo espelho nem camada de
mapeamento entre domínio e persistência.

## Consequências

**A favor.** Elimina três classes e um mapeador por agregado — para `Pauta`,
`SessaoVotacao` e `Voto`, cerca de metade do código da camada de dados. Menos
código é menos superfície para bug.

**Contra.** O domínio fica acoplado a `jakarta.persistence`. Trocar de ORM
exigiria tocar nas entidades.

**Mitigação.** O acoplamento é a anotações, não a comportamento: as classes não
estendem nada do framework e não chamam a API do Hibernate. O domínio continua
com comportamento próprio (`SessaoVotacao.estaAberta()`,
`ResultadoVotacao.apurar()`), não é um saco de getters. E o ArchUnit impede que o
domínio ganhe dependência de Spring Web.

## Quando reconsiderar

Se o domínio crescer a ponto de as necessidades de modelagem divergirem das de
persistência — agregados grandes, herança complexa, ou a necessidade de
persistir o mesmo agregado em mais de um armazenamento.
