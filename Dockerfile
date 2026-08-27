FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# As dependencias sao baixadas em uma camada propria: enquanto o pom.xml nao
# mudar, o Docker reaproveita o cache e o build seguinte pula o download.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes de integracao exigem Docker; rodam no CI e no `mvn verify` local,
# nao dentro da propria imagem.
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------------------

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuario sem privilegios: um processo comprometido nao ganha root no container.
RUN addgroup -S votacao && adduser -S votacao -G votacao
USER votacao

COPY --from=build /build/target/votacao.jar app.jar

EXPOSE 8080

# MaxRAMPercentage faz a JVM respeitar o limite de memoria do container em vez
# de enxergar a memoria do host inteiro, evitando OOMKill sob carga.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
