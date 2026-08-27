# ADR 0007 — Inversão de dependência sem modelo espelho

**Status:** aceita · **Data:** 2026-08-27 · **Refina:** [ADR 0001](0001-entidade-jpa-como-modelo-de-dominio.md)

## Contexto

A [ADR 0001](0001-entidade-jpa-como-modelo-de-dominio.md) rejeitou a arquitetura
hexagonal ortodoxa em nome da simplicidade — critério que o enunciado lista em
primeiro lugar. Uma revisão posterior mostrou que, ao rejeitar a *cerimônia*, o
projeto também deixou passar três violações que não têm nada a ver com
simplicidade e sim com direção de dependência:

1. **`HttpStatus` nas exceções de domínio.** As regras de negócio conheciam
   códigos HTTP. A mesma regra, exposta por mensageria, continuaria valendo e não
   teria status algum.
2. **`AssociadoValidator` importava `UserInfoClient`.** Uma regra de negócio
   dependia da classe concreta de um cliente HTTP.
3. **`domain/repository/*` estendia `JpaRepository`.** O domínio dependia do
   Spring Data e herdava mais de vinte métodos que ninguém chama — incluindo
   `deleteAll()`.

Nenhuma das três economizava complexidade. Eram acoplamento acidental.

## Decisão

Aplicar **inversão de dependência de verdade**, mantendo a decisão de **não**
duplicar o modelo.

**O que foi invertido:**

- `TipoErro` (enum de domínio) substitui `HttpStatus` nas exceções. A tradução
  para status vive em `MapeadorDeStatus`, na camada de API.
- `ConsultaAptidaoParaVotar` é uma porta declarada em `application.port`;
  `UserInfoClient` passou a implementá-la.
- `PautaRepository`, `SessaoVotacaoRepository` e `VotoRepository` são portas
  declaradas pelo domínio, com **apenas os métodos usados**. Os adaptadores vivem
  em `infrastructure.persistence`.
- `Pagina<T>` é um record de domínio, para que as portas não precisem falar
  `org.springframework.data.domain.Page`.

**O que continua como estava:** as entidades seguem anotadas com JPA e servindo
como modelo de domínio.

## Consequências

**A favor.**

- O pacote `domain` não depende de nenhuma linha de Spring — regra verificada por
  ArchUnit, não apenas afirmada no README.
- Trocar o serviço externo por um cadastro próprio da cooperativa é escrever uma
  implementação da porta. Nenhuma regra muda, nenhum teste de regra é reescrito.
  O `AssociadoValidatorTest` já demonstra isso: ele testa a regra sem saber que
  existe um serviço REST do outro lado.
- Interface Segregation nos repositórios: o domínio declara quatro métodos em vez
  de herdar vinte.
- Open/Closed no tratamento de erros: uma regra nova reutiliza uma natureza
  existente e `MapeadorDeStatus` não muda.

**Contra.**

- Três interfaces a mais (as portas de repositório) e um enum a mais.
- Os adaptadores precisam delegar. Custo mitigado pelo formato adotado: a
  interface estende a porta **e** `JpaRepository`, e os métodos são `default`
  delegando ao Spring Data. Isso entrega a inversão **sem nenhuma classe
  adaptadora escrita à mão** — três classes cujo corpo inteiro seria delegação.

**O custo que continua recusado.** Duplicar as três entidades em modelos de
persistência espelhados, com mapeadores entre eles, dobraria a camada de dados
para eliminar uma dependência de *anotações*. `jakarta.persistence` é uma
especificação declarativa, sem modelo de programação próprio — materialmente
diferente de `JpaRepository`, que traz uma API inteira e semântica transacional.
É aí que o custo/benefício se inverte, e é aí que a linha foi traçada.

## Verificação

`ArquiteturaTest` tem sete regras que fazem o build falhar se a direção das
dependências for invertida — incluindo a que proíbe `org.springframework..` no
domínio, exatamente a brecha por onde `HttpStatus` havia entrado.
