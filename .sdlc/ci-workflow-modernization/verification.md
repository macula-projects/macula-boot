# 验证报告：CI 工作流现代化

日期：2026-09-22

变更：`ci-workflow-modernization`

基线：`ae3c8eadbe8dc07917b137767c8c875979e42972`（`origin/main`）

受测实现：`fc8941d51956614c5ada2c4a9e72b4ecfc3564ae`（`feat/ci-workflow-modernization`）

环境：macOS ARM64、Tencent Kona JDK 17.0.17、Apache Maven 3.9.6、Go `actionlint` 1.7.12；GitHub 托管 Runner 使用 `ubuntu-latest` 和 Redis 7 Service。

## 结论

验证尚未通过。Pull Request 门禁行为正确，但合并后的首次 Snapshot 发布暴露了 `actions/setup-java@v6` 与仓库 `maven-gpg-plugin:3.0.1` 的不兼容，当前不能进入审查交接。

本次验证未合并 PR、未创建 Tag、未触发 Release，也未发布 Snapshot 或 Maven Central 正式版本。

## 本地工作流校验

执行命令：

```shell
go run github.com/rhysd/actionlint/cmd/actionlint@v1.7.12 \
  .github/workflows/verification.yml \
  .github/workflows/snapshot.yml \
  .github/workflows/release.yml
git diff --check
```

两条命令退出状态均为 `0`；`actionlint` 无错误输出，`git diff --check` 无输出。

确定性结构断言同时证明：

- 三个工作流均使用 `actions/checkout@v7` 与 `actions/setup-java@v6`，不存在范围内的 v3/v4 引用。
- 三个工作流均显式声明 `permissions: contents: read`。
- Verification 保留 `pull_request` 和 `push.branches: [main]`。
- Snapshot 只暴露 `workflow_call`，没有直接的 `push` 或 `pull_request` 入口。
- Snapshot 调用任务使用 `needs: [checkstyle, verify]`，并限制为 `push` 与 `refs/heads/main`。
- Release 保留 `workflow_dispatch` 及 `released`、`prereleased` 触发器。
- Java 17、Redis Service、Maven Verify、两个 deploy 命令、`deploy` Profile、GPG 输入和 Sonatype 环境变量映射保持不变。

## Maven 全仓验证

执行命令：

```shell
mvn --batch-mode verify
```

退出状态：`0`。

实际输出：

```text
[INFO] Reactor Summary for macula-boot 6.1.0-SNAPSHOT:
...
[INFO] Macula Boot Parent ................................. SUCCESS
...
[INFO] Macula Boot Archetype .............................. SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:05 min
[INFO] Finished at: 2026-09-22T22:12:35+08:00
```

本次生成的 Surefire/Failsafe XML 汇总：

```text
reports=64 tests=186 failures=0 errors=0 skipped=3
```

三个跳过项均为仓库已有的外部基础设施测试：

```text
dev.macula.boot.starter.kafka.KafkaProducerIT#testSend
dev.macula.boot.starter.rocketmq.test.OrderServiceIT#testSendTxMsg
dev.macula.boot.starter.tinyid.TinyIdClientIT#testNextId
```

## Pull Request 调度证明

Pull Request：[PR #33](https://github.com/macula-projects/macula-boot/pull/33)

Maven Verification 运行：[35738951988](https://github.com/macula-projects/macula-boot/actions/runs/35738951988)

该运行的事件为 `pull_request`，受测 SHA 为 `fc8941d51956614c5ada2c4a9e72b4ecfc3564ae`，结论为 `success`。实际 Job 结果：

```text
Checkstyle        success
Maven Verify      success
Publish Snapshot  skipped
```

对应 Job：

- [Checkstyle](https://github.com/macula-projects/macula-boot/actions/runs/35738951988/job/106783355809)
- [Maven Verify](https://github.com/macula-projects/macula-boot/actions/runs/35738951988/job/106783356157)
- [Publish Snapshot（skipped）](https://github.com/macula-projects/macula-boot/actions/runs/35738951988/job/106784858194)

PR 全部检查汇总为 `6 successful, 1 skipped, 0 failing`。这证明 GitHub 接受可复用工作流语法，PR 会运行 Checkstyle 和 Maven Verify，而 Snapshot 调用任务会被条件跳过，不会产生部署运行。

## 合并后首次 main 运行

PR #33 在验证报告提交前由维护者合并，合并提交为 `ee30cf88d4883a484ebb419e3c02cd5b51677b0c`。对应的 [Maven Verification 运行 35739822914](https://github.com/macula-projects/macula-boot/actions/runs/35739822914) 证明任务门禁顺序正确：

```text
Checkstyle        success（9s）
Maven Verify      success（3m58s）
Publish Snapshot  在两个前置任务成功后启动
```

首次 Snapshot 尝试因 Maven Central 下载 Guava 时连接提前结束而失败。仅重跑失败 Job 后，依赖解析成功，但 GPG 签名稳定失败：

```text
gpg: signing failed: No pinentry
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-gpg-plugin:3.0.1:sign
[INFO] Macula Boot Parent ................................. FAILURE
```

`actions/setup-java@v6` 官方说明其 GPG passphrase 改用 `gpg.passphraseEnvName`，要求 `maven-gpg-plugin` 3.2.0 或更高版本；仓库根 POM 与 `macula-boot-parent/pom.xml` 均固定为 3.0.1。修复需要升级 Maven GPG Plugin，并将工作流切换到 v6 的 `server-username-env-var`、`server-password-env-var` 和 `gpg-passphrase-env-var` 输入。这属于已接受规格“不得修改 Maven”之外的变更，尚待人工确认。

## 需求与证明对应关系

- 需求 1、2、8、9：由 `actionlint`、结构断言和 PR 实际运行共同证明。
- 需求 3：PR 的 Checkstyle、Maven Verify 成功，Publish Snapshot 明确为 `skipped`；发布 Secrets 只位于未执行的可复用工作流调用任务。
- 需求 4、5：Snapshot Job 同时依赖两个验证 Job，且没有 `always()` 等绕过成功依赖语义的条件。
- 需求 6：Java、Maven 命令、Profile、Server 配置、GPG 与 Sonatype 映射的差异检查无变化。
- 需求 7：Build 阶段发现规格漏列了签名实际使用的既有 `GPG_SECRET`。经人工确认的实现按 `plan.md` 偏差记录显式传递四个既有发布 Secrets，未新增 Secret，也未扩大权限。
- 需求 10：可复用 Snapshot 未指定其他 `ref`，checkout 继承调用方提交上下文；首次真实 `main` 发布的 SHA 证明只能在人工合并后观察，本阶段未通过合并或发布进行测试。

## 受保护配置与 Evals

本变更未修改 `CLAUDE.md`、`AGENTS.md`、Skill 或 Hook，因此没有适用的配置 Eval，也未虚构 Eval 命令。

## 交接

Stage 4 当前为红色，必须回到 Build 修复 GPG 签名兼容性并重新验证；不得进入 `sdlc-deploy`。本报告不自行批准依赖范围扩展、审查、合并、Release 或发布。
