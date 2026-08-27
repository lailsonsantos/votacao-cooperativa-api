# Rodando o projeto no IntelliJ IDEA

## Abrir

**File → Open** e selecione o arquivo `pom.xml` (não a pasta). O IntelliJ
reconhece o projeto Maven e baixa as dependências.

## 1. SDK do projeto: Java 21

**File → Project Structure → Project → SDK**

Se não houver um JDK 21 na lista, use **Add SDK → Download JDK** e escolha
qualquer distribuição na versão 21. O projeto usa `records`, *pattern matching* e
blocos de texto; com JDK 17 o código não compila.

Um `.java-version` no repositório já aponta a versão para quem usa jenv ou asdf.

## 2. Lombok: habilitar o processamento de anotações

**Este é o passo que mais causa confusão.** Sem ele, a IDE mostra o projeto
inteiro em vermelho — "cannot find symbol: log", "constructor não existe" — ainda
que `./mvnw verify` passe normalmente no terminal.

O motivo é que o Lombok gera construtores, getters e o campo `log` durante a
compilação. Se a IDE não roda o processador de anotações, ela não enxerga nada
disso e reporta erros que não existem.

**Settings → Build, Execution, Deployment → Compiler → Annotation Processors →
marque `Enable annotation processing`.**

O plugin do Lombok já vem embutido nas versões recentes do IntelliJ. Em versões
antigas, instale por **Settings → Plugins → Marketplace → "Lombok"**.

## 3. Rodar

O repositório versiona as configurações em `.run/`, então elas aparecem prontas
na lista de execução, no canto superior direito:

| Configuração | O que faz | Precisa de Docker |
|---|---|---|
| **API - perfil local (H2)** | Sobe a API com banco em memória | Não |
| **API - PostgreSQL do Docker** | Sobe a API contra o Postgres do compose | Sim |
| **Testes - unitarios (rapido)** | Só os unitários, em segundos | Não |
| **Testes - suite completa** | Tudo: integração, cobertura, formatação, análise estática | Sim |
| **Formatar codigo (Spotless)** | Corrige a formatação | Não |

Comece por **API - perfil local (H2)**: é a única que funciona sem mais nada
instalado.

| | |
|---|---|
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Telas do Anexo 1 | http://localhost:8080/api/v1/telas |
| Console do H2 | http://localhost:8080/h2-console |

No console do H2 use JDBC `jdbc:h2:mem:votacao`, usuário `sa`, sem senha.

Para a configuração com PostgreSQL, suba o banco antes:

```bash
docker compose up banco
```

## 4. Testes de integração pela IDE

Os testes `*IT` sobem um PostgreSQL real via Testcontainers e exigem o **Docker
Desktop em execução**.

Eles funcionam ao serem executados direto pela IDE — botão direito no arquivo,
ou o ícone verde ao lado do método. Isso não é automático: o Docker Engine 29
elevou a versão mínima da API aceita e recusa a versão que o `docker-java`
negocia por padrão. A propriedade que corrige isso é definida em
`IntegracaoTest`, e não na configuração do Maven, justamente para valer também
quando a IDE executa o teste — que é como se depura um teste que falhou.

## 5. Executar requisições sem sair da IDE

O arquivo [`docs/api.http`](api.http) abre no editor com um botão de execução ao
lado de cada requisição. Cobre o fluxo inteiro: cadastrar pauta, abrir sessão,
votar, ver o duplicado ser recusado e apurar o resultado — além das telas do
Anexo 1.

O suporte é nativo no IntelliJ IDEA Ultimate. No Community, use a extensão
**REST Client** do VS Code ou o `curl` do README.

## Problemas comuns

### `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`

Erro do Lombok, não do projeto. Ele manipula estruturas internas do compilador,
então cada versão só conhece os JDKs lançados até ela. Quando a IDE compila com
um JDK mais novo do que a versão do Lombok suporta, a inicialização do
processador falha exatamente com essa mensagem.

O sintoma característico é o build passar no terminal e falhar na IDE — porque
são JDKs diferentes.

**O projeto já fixa a versão mais recente do Lombok**, o que elimina a
dependência de qual JDK a IDE usa. Se o erro aparecer, é porque o IntelliJ ainda
está com a versão anterior em cache:

1. **Recarregue o projeto Maven:** aba *Maven* (lateral direita) → ícone de
   recarregar, ou botão direito no `pom.xml` → **Maven → Reload project**.
2. **Build → Rebuild Project.**
3. Persistindo: **File → Invalidate Caches → Invalidate and Restart**.

Se ainda assim ocorrer, alinhe o JDK da IDE ao do projeto:

- **File → Project Structure → Project → SDK** deve ser **21**.
- **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE**
  deve ser **Project SDK** (não o *JetBrains Runtime* embutido, que costuma ser
  outra versão).

### `NoSuchMethodError` em `google-java-format` durante o `spotless:check`

Mesma causa do erro do Lombok, em outra ferramenta. O formatador também usa APIs
internas do compilador, e o **JBR embutido do IntelliJ já está no JDK 25** —
mais novo do que versões antigas conhecem.

As versões no `pom.xml` já são as que suportam o JDK 25. Se o erro aparecer,
recarregue o projeto Maven para que a IDE pegue as versões novas.

O padrão vale para qualquer ferramenta que toque no compilador: quando algo falha
na IDE e passa no terminal, o primeiro suspeito é a diferença de JDK.

### Fazer a IDE usar exatamente o mesmo build do terminal

Se as diferenças entre IDE e terminal incomodarem, delegue a compilação ao Maven:

**Settings → Build, Execution, Deployment → Build Tools → Maven → Runner →
marque `Delegate IDE build/run actions to Maven`.**

O IntelliJ passa a chamar o Maven em vez do próprio compilador, e o resultado
fica idêntico ao de `./mvnw verify`. O custo é um build um pouco mais lento.


**"Cannot find symbol: log" ou "constructor não existe", mas o Maven compila.**
Processamento de anotações desabilitado. Volte ao passo 2.

**Os testes `*IT` falham com "Could not find a valid Docker environment".**
O Docker Desktop não está rodando. Suba e execute de novo.

**"invalid target release: 21".**
SDK do projeto em versão anterior. Volte ao passo 1.

**Qualquer erro que aconteça na IDE e não no terminal.**
Quase sempre é diferença de JDK: o IntelliJ usa o JBR embutido (hoje JDK 25) e o
terminal usa o `JAVA_HOME` (JDK 21). O build foi verificado nas duas versões, mas
se algo destoar, alinhe o *Maven Runner JRE* ao *Project SDK*.

**O build falha em `spotless:check` depois de eu editar código.**
Rode a configuração **Formatar codigo (Spotless)**. Se preferir formatar ao
salvar, o plugin *Save Actions* ou o formatador do google-java-format resolvem —
mas o comando do Maven é a fonte da verdade, porque é ele que o CI executa.

**A porta 8080 já está em uso.**
Edite a configuração e acrescente `--server.port=8081` em *Program arguments*, ou
libere a porta.
