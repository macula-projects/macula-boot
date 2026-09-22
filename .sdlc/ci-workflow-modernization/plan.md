# 计划：升级 CI Actions 并为 Snapshot 发布增加验证门禁（来源：spec.md，2026-09-22）
Status: accepted

## 变更文件
- `.github/workflows/verification.yml`：保留 Pull Request 与 `main` push 验证入口；升级 Action 版本；增加仅在 `main` push 且 Checkstyle、Maven Verify 均成功后调用 Snapshot 可复用工作流的任务。
- `.github/workflows/snapshot.yml`：从独立的 `push` 工作流改为 `workflow_call` 可复用工作流；显式声明三个发布 Secrets；升级 Action 版本并保持 Maven Snapshot deploy 命令和签名配置不变。
- `.github/workflows/release.yml`：保留现有手动和 GitHub Release 触发器；升级 Action 版本并显式声明只读内容权限。
- `.sdlc/ci-workflow-modernization/plan.md`：记录已接受的实施契约，以及实施中实际发生的偏差。

## 工作顺序
1. 重新检查分支、工作区和三个工作流的当前内容，保存 Release 触发器、Java 版本、Maven 命令、Profile、凭据名称与环境变量映射的基线。
2. 先修改 `snapshot.yml`：将事件入口替换为 `workflow_call`，只声明 `OSSRH_USER`、`OSSRH_PASSWORD`、`GPG_PASSWORD` 三个必需 Secrets，添加 `contents: read`，升级为 `checkout@v7` 和 `setup-java@v6`，并保持发布步骤行为不变。
3. 修改 `verification.yml`：保留 `pull_request` 与 `push.branches: [main]`，把现有两个任务中的 Action 升级到 v7/v6；新增同时 `needs: [checkstyle, verify]` 的 Snapshot 调用任务，以 `push` 事件和 `refs/heads/main` 为执行条件，并逐个映射三个发布 Secrets。
4. 修改 `release.yml`：不改变 `workflow_dispatch`、`released`、`prereleased` 或 Maven deploy 行为，只增加 `contents: read` 并升级为 `checkout@v7`、`setup-java@v6`。
5. 对三个 YAML 执行语法解析；若本机存在 `actionlint` 则执行，否则记录缺失，并使用结构化搜索和断言验证触发器、任务依赖、条件、Secrets 接口、Action 版本、Java 17、Maven 命令与权限。
6. 对最终 diff 执行 `git diff --check`，逐项对照 Spec 的 10 条需求与非目标；把任何文件、顺序、行为或验证变化记录到本计划的“偏差”章节。
7. 实施完成后停止在 Build 门禁。正式测试阶段再推送分支并通过 Pull Request 证明 GitHub 能解析工作流、PR 只运行验证且发布任务被跳过；未经人工批准不得合并或触发发布。

## 风险
- 可复用工作流只能在 Job 级通过 `uses: ./.github/workflows/snapshot.yml` 调用；错误地混用普通 Job 字段会使 GitHub 拒绝整个工作流。
- `workflow_call` Secrets 声明与调用方映射必须完全一致；名称或必填配置错误会在 `main` push 时阻断 Snapshot 发布。
- 发布任务的事件和引用条件如果过宽，会重新向 Pull Request 暴露发布路径；如果过窄，则可能让 `main` 不再发布 Snapshot。
- Snapshot 必须依赖两个验证任务。遗漏任一 `needs` 或使用允许失败后继续执行的条件，会破坏发布门禁。
- 可复用工作流必须使用调用方的同一个 SHA；显式切换到默认分支或其他引用可能发布未验证代码。
- Action 主版本升级依赖 GitHub 托管 Runner 的当前 Runtime；本变更不支持未声明的自托管 Runner。
- Snapshot 运行将显示在 Maven Verification 调用链下；依赖旧独立工作流运行名称的外部自动化可能需要后续调整。
- 回滚方式是恢复三个工作流的上一版本；不得通过重新启用 PR 发布或降低权限边界来临时绕过失败。

## 证明
- YAML/Action 证明：三个文件均能被 YAML 解析器读取；可用时 `actionlint` 零错误；Pull Request 上 GitHub 工作流解析和调度成功。
- 版本证明：三个范围内工作流只包含 `actions/checkout@v7` 和 `actions/setup-java@v6`，不存在 v3/v4 遗留引用。
- 触发器证明：Verification 同时包含 `pull_request` 和 `push` 到 `main`；Snapshot 只包含 `workflow_call`；Release 仍包含 `workflow_dispatch`、`released`、`prereleased`。
- 门禁证明：Snapshot 调用任务同时依赖 `checkstyle`、`verify`，仅允许 `push/main`，并逐个传递三个具名 Secrets；PR 检查集中发布任务为 skipped，且不会产生 Snapshot 部署运行。
- 行为保持证明：Java 仍为 17；Redis Service、Checkstyle 脚本、`mvn --batch-mode verify`、两个 `mvn clean deploy --batch-mode -Pdeploy -DskipTests=true` 命令、GPG 输入和 Sonatype 环境变量映射保持不变。
- 权限证明：三个工作流都显式声明 `contents: read`，没有新增写权限或不相关 Secrets。
- SHA 证明：可复用 Snapshot checkout 使用调用者上下文；人工批准合并后的首次 `main` 运行中，Verification SHA、调用的 Snapshot SHA 和合并 SHA 完全一致。
- 边界证明：实施和 PR 验证期间不运行 Release、不创建 Tag、不合并、不发布 Snapshot；首次真实发布证明只能在后续人工合并后观察。

## 偏差
- 实现校验发现 `snapshot.yml` 的 `actions/setup-java` 通过 `gpg-private-key` 实际使用了现有的 `GPG_SECRET`。为保持原有签名发布行为并让可复用工作流通过 GitHub Actions 表达式校验，Snapshot 接口及调用方改为显式传递四个发布 Secrets，而不是设计中误记的三个；未新增仓库 Secret，也未扩大权限。
- 合并后的首次 Snapshot 运行证明 `actions/setup-java@v6` 通过 `gpg.passphraseEnvName` 传递签名口令，要求 `maven-gpg-plugin` 3.2.0 或更高版本；仓库原有 3.0.1 因无法读取该环境变量而报 `No pinentry`。经人工确认，根 POM 与 Parent POM 的插件升级到 Maven Central 当前稳定版 3.2.8，Snapshot/Release 同步改用 v6 的 `*-env-var` 输入名；发布命令、Secret、环境变量和权限不变。
