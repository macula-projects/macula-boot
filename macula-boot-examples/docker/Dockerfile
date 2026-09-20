# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-17 AS builder

ARG MODULE
WORKDIR /workspace

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    test -n "${MODULE}" \
    && test -f "macula-boot-examples/${MODULE}/pom.xml" \
    && mvn -pl "macula-boot-examples/${MODULE}" -am package -Dmaven.test.skip=true \
    && jar_file="$(find "macula-boot-examples/${MODULE}/target" -maxdepth 1 -type f -name '*.jar' \
       ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)" \
    && test -n "${jar_file}" \
    && cp "${jar_file}" /tmp/application.jar

FROM eclipse-temurin:17.0.16_8-jre-jammy AS runtime

LABEL org.opencontainers.image.authors="rainsoft"

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system macula \
    && useradd --system --gid macula --home-dir /home/macula --create-home macula

WORKDIR /app
RUN mkdir -p /app/polaris/backup /app/polaris/logs \
    && chown -R macula:macula /app
COPY --from=builder --chown=macula:macula /tmp/application.jar /app/application.jar

USER macula
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
