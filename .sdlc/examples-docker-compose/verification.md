# Verification Report

Date: 2026-09-21

Change: `examples-docker-compose`

Baseline commit: `d36367b5901fedc0150a258dc65e658ce2eafc22`

Change under test: the baseline plus the originator-approved, uncommitted 2026-09-21 application-port revision recorded in `spec.md` and `plan.md`, including the confirmed application runtime change to pinned BellSoft Liberica JRE 17 on Alpaquita glibc.

Environment: macOS, OrbStack Docker Engine 29.4.0 on `linux/arm64`, Docker Compose v5.1.2 using the Compose v2 `docker compose` interface.

## Result

`Verification is green`.

The accepted platform limitations remain in force: Windows Docker Desktop, native `linux/amd64` runtime, and Alibaba host-default runtime were explicitly waived by the originator in `plan.md`. They are not claimed as executed. Image manifests cover both `linux/amd64` and `linux/arm64`; runtime smoke tests in this report were executed on macOS/ARM64.

## Project tests

Command:

```shell
mvn verify
```

Exit status: `0`.

Relevant output:

```text
[INFO] Reactor Summary for macula-boot 6.0.1-SNAPSHOT:
...
[INFO] Macula Boot Examples ............................... SUCCESS
[INFO] Macula Example Alibaba Gateway ..................... SUCCESS
[INFO] Macula Example Alibaba Consumer .................... SUCCESS
[INFO] Macula Example Alibaba Provider .................... SUCCESS
[INFO] Macula Example Tencent Gateway ..................... SUCCESS
[INFO] Macula Example Tencent Provider .................... SUCCESS
[INFO] Macula Example Tencent Consumer .................... SUCCESS
[INFO] BUILD SUCCESS
[INFO] Total time:  01:05 min
[INFO] Finished at: 2026-09-21T11:11:39+08:00
```

Fresh Surefire/Failsafe XML aggregation:

```text
tests=174 failures=0 errors=0 skipped=3
```

The three skips are documented external-infrastructure tests:

```text
KafkaProducerIT#testSend: 需要启动Kafka服务才能运行此测试
OrderServiceIT#testSendTxMsg: 需要启动RocketMQ才能测试
TinyIdClientIT#testNextId: 需要启动tinyid服务器才能运行此测试
```

## Proof and verification-strategy results

### Requirements 1-6: Compose topology and health

Commands executed included:

```shell
docker compose config --profiles
docker compose config --services
docker compose --profile alibaba config --services
docker compose --profile tencent config --services
docker compose --profile alibaba --profile tencent config --services
docker compose --profile alibaba --profile tencent config --format json
docker compose --profile alibaba --profile tencent up -d --wait --wait-timeout 300
docker compose --profile alibaba --profile tencent ps -a
```

Relevant output:

```text
profiles=alibaba,tencent
default_services=redis,mysql
alibaba_services=redis,mysql,nacos,nacos-init,macula-example-alibaba-provider1,macula-example-alibaba-consumer,macula-example-alibaba-gateway
tencent_services=mysql,polaris,macula-example-tencent-provider,macula-example-tencent-consumer,redis,macula-example-tencent-gateway
healthcheck_count=10
```

The post-revision fresh dual-profile run reached healthy status for all ten long-running services. `nacos-init` exited with code `0`. Startup output showed the required MySQL/Redis -> Nacos/Polaris -> provider -> consumer -> gateway dependency order.

Structured Compose output showed all 16 published ports bound to `127.0.0.1`, named volumes `mysql-data` and `redis-data`, no fixed `platform`, no host-specific path, and no mounted host shell script. No `latest` image reference was found.

### Requirements 7-8: observable Alibaba and Tencent behavior

The original fresh empty-volume single-profile runs produced the exact intended service sets and no services from the other profile. After the approved port revision, a second fresh empty-volume dual-profile run revalidated both application chains together.

Alibaba command and output:

```shell
ALIBABA_GATEWAY_HTTP_PORT=15000 \
  docker compose --profile alibaba --profile tencent up -d --wait --wait-timeout 300
curl --fail-with-body --silent --show-error --write-out '\nHTTP %{http_code}\n' \
  'http://127.0.0.1:15000/consumer/api/v1/consumer/echo/demo?str=hello'
```

```text
macula-example-alibaba-consumer    Up (healthy)  127.0.0.1:5010->5010/tcp
macula-example-alibaba-provider1   Up (healthy)  127.0.0.1:5020->5020/tcp
macula-example-alibaba-gateway     Up (healthy)  127.0.0.1:5443->5443/tcp, 127.0.0.1:15000->5000/tcp
{"success":true,"code":"00000","msg":"请求成功","cause":null,"data":"Hello hello, test=365, by anonymousUser, at 2026-09-21 02:03:45,demo"}
HTTP 200
```

Tencent command and output:

```shell
curl --fail-with-body --silent --show-error --write-out '\nHTTP %{http_code}\n' \
  'http://127.0.0.1:4000/consumer/api/v1/consumer/echo'
```

```text
macula-example-tencent-consumer   Up (healthy)  127.0.0.1:4010->4010/tcp
macula-example-tencent-provider   Up (healthy)  127.0.0.1:4020->4020/tcp
macula-example-tencent-gateway    Up (healthy)  127.0.0.1:4000->4000/tcp
{"success":true,"code":"00000","msg":"请求成功","cause":null,"data":"Hello consumer, by anonymousUser, at 2026-09-21 02:03:45"}
HTTP 200
```

Both responses were HTTP 200 in the post-revision dual-profile run. TCP checks also reached all six application host ports: `4000`, `4010`, `4020`, `5010`, `5020`, and `5443`. Nacos contained `MACULA5` and `SENTINEL`, and MySQL contained `nacos_config` and `polaris_server`.

The default Compose render was checked separately and preserves the approved application mappings:

```text
Alibaba gateway HTTP/HTTPS=5000/5443 provider=5020 consumer=5010
Tencent gateway=4000 provider=4020 consumer=4010
```

Host port `5000` was occupied by an unrelated local service, so only the host side of the Alibaba HTTP mapping was overridden to `15000` for runtime verification. The container port, health check, service-to-service URLs, and default rendered host port all remained `5000`.

### Requirement 9: Middleware-only and overridable ports

README commands were executed with host-port overrides because existing user services occupied 3306, 6379, 8091, 8093, 8848, and 9848. Container-internal addresses remained unchanged.

```text
alibaba_middleware_all=mysql,nacos,nacos-init,redis
alibaba_middleware_running=mysql,nacos,redis
alibaba_nacos_init_exit=0
tencent_middleware_all=mysql,polaris,redis
tencent_middleware_running=mysql,polaris,redis
applications_and_nacos_absent=true
```

Host TCP checks reached every overridden MySQL, Redis, Nacos, and Polaris port. A separate rendered configuration proved port-variable overrides, including 23306, 26379, 28848, 29848, and 28091. The default render preserves 3306, 6379, 8848, 9848, 8091, and 8093.

The Tencent provider was then started from the host:

```shell
POLARIS_SERVER_ADDR=grpc://127.0.0.1:18091 \
POLARIS_NACOS_SERVER_ADDR=127.0.0.1:28049 \
  mvn -pl macula-boot-examples/macula-example-tencent-provider spring-boot:run
```

Relevant output:

```text
connection ... clusterType=SERVICE_DISCOVER_CLUSTER, host='127.0.0.1', port=18091 created
Success to connect to server [127.0.0.1:28049] on start up
{"status":"UP"}
HTTP 200
{"success":true,"code":"00000","msg":"请求成功","cause":null,"data":"Hello host, by anonymousUser, ..."}
HTTP 200
[INFO] BUILD SUCCESS
```

### Requirements 10-11: pinned multi-architecture images and portability

Command:

```shell
docker buildx imagetools inspect --raw <image>
```

Manifest results:

```text
mysql:8.0.43                                      linux/amd64,linux/arm64
redis:7.4.5                                       linux/amd64,linux/arm64
nacos/nacos-server:v2.5.1-slim                    linux/amd64,linux/arm64
curlimages/curl:8.16.0                            linux/amd64,linux/arm64
polarismesh/polaris-server:v1.18.1                linux/amd64,linux/arm64
maven:3.9.11-eclipse-temurin-17                   linux/amd64,linux/arm64
bellsoft/liberica-runtime-container:jre-17.0.20.1_1-glibc linux/amd64,linux/arm64
```

Some images also publish additional architectures. No fixed `platform`, host-specific path, GNU-only host command, or host shell script is used. Runtime execution was ARM64 only under the accepted waiver.

### Requirements 12-13: local security and persistence

All published ports rendered with `host_ip=127.0.0.1`. Credentials are the documented local example values. The isolated special-character regression used `MYSQL_ROOT_PASSWORD='a&b|c'` with clean volumes:

```text
rendered_password=a&b|c
polaris_service_rows=1
```

The isolated Compose project and its volume were removed after the test.

Persistence output:

```text
after_normal_down=retained
after_down_v=
nacos_init_exit=0
schemas=nacos_config,polaris_server
```

This proves ordinary `down` retained Redis state while `down -v` removed it and successfully reinitialized both databases and Nacos namespaces.

### Requirement 14: documentation

Both READMEs were checked against the actual service and profile names. They contain copyable commands for Alibaba, Tencent, dual-profile, both Middleware-only modes, status/log inspection, stop, destructive reset, host-run applications, architecture scope, port overrides, and local-credential warnings.

One diagnostic command, `docker compose up --wait redis nacos-init`, returned exit status `1` because Compose treats the successfully completed one-shot `nacos-init` container as no longer running. The documented command is `docker compose up -d redis nacos-init`; it was rerun and verified with `nacos-init` exit code `0` plus independent health checks.

### Requirement 15: existing host defaults

The freshly built JAR resources retain these defaults:

```text
NACOS_SERVER_ADDR:127.0.0.1:8848
NACOS_NAMESPACE:MACULA5
POLARIS_SERVER_ADDR:grpc://127.0.0.1:8091
POLARIS_NACOS_SERVER_ADDR:127.0.0.1:18849
POLARIS_NAMESPACE:macula-dev
Alibaba application ports: gateway HTTP/HTTPS 5000/5443, provider 5020, consumer 5010
Tencent application ports: gateway 4000, provider 4020, consumer 4010
```

Tencent host execution was proven using documented port overrides. Alibaba host-default runtime remains covered by the accepted originator waiver and is not claimed as executed.

### Application images

Command:

```shell
docker compose --profile alibaba --profile tencent build
```

Exit status: `0`; all six application images reported `Built`.

Each image reported:

```text
user=macula
openjdk version "17.0.20.1" 2026-08-18 LTS
uid=100
files=/app/application.jar,/app/polaris,/app/polaris/backup,/app/polaris/logs
history_sensitive_hits=0
```

No `.git`, source tree, host build output, password, secret-key, or token was found in runtime paths or image build history.

### Liberica runtime re-verification

The runtime-image change was verified from fresh commands rather than inherited from the earlier Temurin run.

Commands included:

```shell
mvn verify
docker compose -p macula-liberica-verify --profile alibaba --profile tencent build
docker compose -p macula-liberica-verify --profile alibaba --profile tencent up -d --wait --wait-timeout 300 --no-build
docker compose -p macula-liberica-verify --profile alibaba --profile tencent down
docker compose -p macula-liberica-verify up -d redis nacos-init polaris
docker compose -p macula-liberica-verify --profile alibaba --profile tencent down -v --remove-orphans
```

The isolated Compose project used free high host ports because unrelated user services occupied several documented defaults. Container ports and service-to-service addresses were unchanged. Fresh output included:

```text
six_application_images=Built
application_runtime=openjdk 17.0.20.1
application_uid=100
dual_profile_long_running_services=10 healthy
nacos_init_exit=0
alibaba_gateway=HTTP 200, provider echo plus demo
tencent_gateway=HTTP 200, provider echo for consumer
namespaces=MACULA5,SENTINEL
schemas=nacos_config,polaris_server
middleware_states=healthy,healthy,healthy,healthy
middleware_application_containers=0
after_normal_down_redis_marker=retained
remaining_isolated_containers=0
remaining_isolated_volumes=0
```

The pinned Liberica runtime manifest reported `linux/amd64` and `linux/arm64`. All six rebuilt images contained `curl`, ran as the non-root `macula` user, and retained writable `/app/polaris/backup` and `/app/polaris/logs` directories. This refresh was executed on macOS/ARM64; it does not remove the accepted Windows and native amd64 runtime limitations.

## Evals and protected configuration

The change did not modify `CLAUDE.md`, a skill, or a hook. No eval suite applies, so no eval command was run.

## Review and limitations

The earlier `REVIEW.md` Bugs, Security, and Compliance passes attached to PR #30 reported no remaining findings after the password-substitution fix and documentation clarification. The newly confirmed Liberica runtime change is verified here and must be included in the next Stage 5 review pass.

During the original verification, Docker's server API stopped responding while pulling images. The hung client requests were stopped, OrbStack was restarted after the user instructed the run to continue, and `docker version` then returned both client and server `29.4.0`. No volume or image data was deleted by the restart. All Docker tests above were executed after recovery.

The Stage 4 test stack and isolated password-test volumes were removed after evidence collection. Existing user Nacos/Polaris containers were not stopped or modified. A host-run Polaris cache directory generated under the Tencent provider module was moved to the macOS Trash. The Git worktree contains the intended port revision, synchronized documentation and SDLC artifacts; it is not claimed as clean.
