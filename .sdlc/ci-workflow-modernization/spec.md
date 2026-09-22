# 规格：升级 CI Actions 并为 Snapshot 发布增加验证门禁（来源：intent.md，2026-09-22）
Status: accepted

## 来源意图
[已接受的意图](intent.md)：使用受支持的 GitHub 官方 Actions，验证 Pull Request 和合并后的 `main` 提交，并且只在 `main` 验证成功后发布 Snapshot。

## 需求
1. `.github/workflows/verification.yml`、`snapshot.yml` 和 `release.yml` 在需要相应 Action 的位置统一使用 `actions/checkout@v7` 和 `actions/setup-java@v6`。
2. Maven Verification 继续对每个 Pull Request 和每次向 `main` 的 push 运行。
3. Pull Request 事件必须运行 Checkstyle 和 Maven Verify，但不得调用 Snapshot 发布，也不得向实际运行的任务提供 Sonatype/GPG 发布 Secrets。
4. 向 `main` push 后，只有同一提交的 Checkstyle 和 Maven Verify 均成功完成，才能发布 Snapshot。
5. Checkstyle 或 Maven Verify 失败、取消或跳过时，必须阻止该提交的 Snapshot 发布。
6. Snapshot 发布继续使用 Java 17、`deploy` Maven Profile、发布构建跳过测试、现有 Maven Central Server 配置、GPG 签名输入以及 Sonatype 凭据映射。
7. Snapshot 工作流只声明并接收其需要的三个现有发布 Secrets：`OSSRH_USER`、`OSSRH_PASSWORD` 和 `GPG_PASSWORD`。
8. Release 工作流继续保留 `workflow_dispatch` 以及 GitHub Release 的 `released`、`prereleased` 触发器、Java 17 和现有 Maven Central 部署行为。
9. 三个工作流均显式使用只读仓库内容权限，除非某个任务确实需要更窄之外的权限；不得新增仓库或令牌权限。
10. 发布任务必须构建并部署通过 Checkstyle 和 Maven Verify 的同一个 `main` 提交。

## 非目标
- 不修改 Java、Maven、Redis Service、Checkstyle、测试选择、部署 Profile、仓库坐标、签名密钥或 Sonatype 凭据。
- 不以减少 CI 消耗为由取消 `main` push 验证。
- 实施验证期间不触发 Release、Tag、合并、Snapshot 发布或 Maven Central 正式发布。
- 不修改分支保护、GitHub Environments、凭据轮换、Release 审批策略或根目录 `release.sh` 流程。
- 不让现有 GitHub Release 工作流依赖 Snapshot 工作流，也不重新设计 Release 生命周期。

## 设计
`verification.yml` 继续作为 `pull_request` 和向 `main` push 的事件入口。现有 `checkstyle` 与 `verify` 任务保持原行为，并升级到受支持的 Action 版本。

`snapshot.yml` 不再作为独立响应 push 的事件工作流，而是通过 `workflow_call` 提供仓库内可复用工作流。它声明三个必需的发布 Secrets，并保留现有 Snapshot 发布任务。其 checkout 使用调用方的提交上下文，因此部署 SHA 与调用方验证通过的 SHA 相同。

`verification.yml` 新增 Snapshot 发布任务，该任务：

- 同时依赖 `checkstyle` 和 `verify`；
- 仅在调用事件为 `push` 且引用为 `refs/heads/main` 时满足执行条件；
- 调用仓库内的 `snapshot.yml`；
- 只传递三个具名发布 Secrets。

GitHub Actions 的依赖语义确保两个前置任务全部成功后才会运行可复用工作流。Pull Request 仍执行两个验证任务，但事件和引用条件会跳过具有发布权限的任务。

`release.yml` 保持现有触发器和 Maven 命令，同时升级 Action 版本并显式声明只读内容权限。

已查阅的治理来源：仓库 `AGENTS.md`、`.agents/rules/dependencies-release.md`、`REVIEW.md` 中的 Bugs、Security 和 Compliance 审查规则、已接受的 Intent，以及 `actions/checkout` v7.0.1 和 `actions/setup-java` v6.0.1 的 GitHub 官方 Release。

## 数据与接口
不涉及应用 API、Schema、持久化或运行时配置变更。

GitHub Actions 契约变更如下：

- `Maven Verification` 继续由 Pull Request 和 `main` push 对外触发。
- `Sonatype Snapshot Repo Deployment` 从独立的 `push` 工作流改为仓库内可复用工作流。
- `main` push 的任务图变为：`checkstyle + verify -> snapshot publish`，且使用相同 SHA。
- Release 工作流的外部触发契约保持不变。
- 发布 Secret 名称和 Maven 环境变量映射保持不变，但通过可复用工作流声明显式限定 Secret 接口。

## 已标记关注点
- Snapshot 工作流历史和检查展示将归入 Maven Verification 调用方，不再表现为独立触发的工作流运行：依赖旧独立运行名称的 Dashboard 或外部自动化可能需要调整，非阻塞。
- `checkout@v7` 和 `setup-java@v6` 使用更新的 JavaScript Runtime 和 Runner 要求：GitHub 托管的 `ubuntu-latest` 满足支持条件，但本规格不覆盖未列出的自托管 Runner，非阻塞。
- 现有 Release 工作流仍可手动启动或由 GitHub Release 触发，不依赖 Maven Verification：保留该行为是已接受的非目标，但 Release 审批仍由维护者负责，非阻塞。

## 验证策略
1. 解析三个 YAML 文件，并在环境可用时运行 `actionlint`；若不可用，则记录该限制，并使用 GitHub 工作流解析器和确定性的结构断言。
2. 搜索全部受维护工作流，证明三个范围内工作流不再存在 `actions/checkout@v3`、`actions/checkout@v4`、`actions/setup-java@v3` 或 `actions/setup-java@v4`。
3. 断言 Verification 仍声明 `pull_request` 和向 `main` 的 `push`。
4. 断言 Snapshot 只暴露 `workflow_call`，准确声明三个发布 Secrets，并且没有直接的 `push` 或 `pull_request` 触发器。
5. 断言调用方发布任务同时依赖 Checkstyle 和 Maven Verify，并且条件限制为 `main` push。
6. 断言 Release 触发器、Java 17、Maven deploy 命令、Profile、签名输入和凭据映射保持不变。
7. 创建 Pull Request，确认 GitHub 能接受工作流，Checkstyle 和 Maven Verify 正常执行，Snapshot 发布被跳过，且不会产生 Snapshot 部署运行。
8. 不通过合并或发布来测试。人工批准合并后，观察首次 `main` 运行，确认 Snapshot 只在两个验证任务成功后启动，并使用合并后的 SHA。
