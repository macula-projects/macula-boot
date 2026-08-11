# 依赖、构建与发布

修改 POM、依赖版本、CI 或发布流程时加载。

## POM 与版本

- 框架及第三方版本集中维护在 `../../macula-boot-parent/pom.xml` 的 properties/dependencyManagement 中。
- 根项目使用 `${revision}`；不要在子模块重复硬编码项目版本。
- 升级 Spring Boot、Spring Cloud、Alibaba 或 Tencent BOM 时，核对版本矩阵，并对受影响 Starter 做编译和测试验证。
- POM 修改后至少运行受影响模块的 `-am test`；dependency management 或插件变更应扩大到全量构建。

## 构建语义

- `mvn test` 执行测试。
- 根 `deploy` profile 默认设置 `skipTests=true`，主要用于 flatten、源码/Javadoc 和签名/发布相关构建。
- 本地验证 deploy 构建时使用 `-Dgpg.skip=true`，避免要求本地签名环境。

## 发布安全

- `../../release.sh` 会切换分支、拉取远端、提交、打 tag、推送，并在最后重置和清理工作区，具有破坏性。
- 除非用户明确要求且工作区状态已核对，否则不要执行 `../../release.sh`。
- 发布前确认版本号、目标分支、凭据、远端和工作区干净状态；发布后报告生成的 commit/tag 与推送结果。
