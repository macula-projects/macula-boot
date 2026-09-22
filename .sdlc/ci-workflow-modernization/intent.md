# 意图：升级 CI Actions 并为 Snapshot 发布增加验证门禁
作者：Rain。Status: accepted。

## 问题
仓库的 GitHub Actions 工作流使用了不一致的 Action 版本，其中 Snapshot 与 Release 流程仍依赖已弃用的版本。向 `main` 推送后，Snapshot 发布还会与验证并行启动，导致尚未完成 Maven 和 Checkstyle 验证的合并提交已经开始发布制品。

## 预期结果
Pull Request 和实际合并到 `main` 的提交都使用受支持的 GitHub Actions 完成验证。只有 `main` 提交验证成功后才发布 Snapshot 制品。现有手动 Release 能力继续保留，且不再产生 Action 版本弃用警告。

## 受影响的用户和系统
仓库维护者、Pull Request 贡献者、Snapshot 下游使用者、Verification、Snapshot 和 Release GitHub Actions 工作流、Maven Central 凭据以及 Sonatype Snapshot 仓库。

## 约束
保持 Java 17 以及现有 Maven 验证和部署语义。继续对 Pull Request 和 `main` push 执行验证。不得向 Pull Request 任务暴露 Maven Central 或 GPG 凭据。实施和验证本变更时不得触发 Release、Tag、合并或制品发布。使用与 `ubuntu-latest` 兼容且当前仍受支持的 GitHub 官方 Action 稳定版本。

## 待确认问题
无。
