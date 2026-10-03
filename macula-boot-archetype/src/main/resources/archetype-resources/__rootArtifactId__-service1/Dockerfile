#set( $symbol_dollar = '$' )
# syntax=docker/dockerfile:1.7
ARG MAVEN_IMAGE=maven:3.9.11-eclipse-temurin-17
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre-jammy
ARG APP_PORT=6003

FROM ${symbol_dollar}{MAVEN_IMAGE} AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp -Plocal -pl ${rootArtifactId}-service1 -am clean package -DskipTests \
    && jar_files=$(find ${rootArtifactId}-service1/target -maxdepth 1 -type f -name '*.jar' \
         ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*.original') \
    && test "$(printf '%s\n' "${symbol_dollar}jar_files" | sed '/^${symbol_dollar}/d' | wc -l)" -eq 1 \
    && cp "${symbol_dollar}jar_files" /tmp/application.jar

FROM ${symbol_dollar}{RUNTIME_IMAGE} AS runtime
ARG APP_PORT
RUN groupadd --system --gid 10001 macula \
    && useradd --system --uid 10001 --gid macula --home-dir /opt/macula --create-home macula
WORKDIR /opt/macula
COPY --from=build --chown=macula:macula /tmp/application.jar application.jar
USER 10001:10001
ENV SERVER_PORT=${symbol_dollar}{APP_PORT} \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE ${symbol_dollar}{APP_PORT}
ENTRYPOINT ["sh", "-c", "exec java ${symbol_dollar}{JAVA_OPTS} -jar /opt/macula/application.jar"]
