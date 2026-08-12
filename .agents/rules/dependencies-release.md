# 依赖、构建与发布

修改 POM、依赖版本、CI 或发布流程时加载。

## POM 与版本

- 框架及第三方版本集中维护在仓库根目录下的 `macula-boot-parent/pom.xml` 的 properties/dependencyManagement 中。
- 根项目使用 `${revision}`；不要在子模块重复硬编码项目版本。
- 升级 Spring Boot、Spring Cloud、Alibaba 或 Tencent BOM 时，核对版本矩阵，并对受影响 Starter 做编译和测试验证。
- 子模块 POM 修改后至少运行受影响模块的 `-am test`；dependency management、测试插件或生命周期变更应扩大到 `mvn clean verify`。

## 构建语义

- Java 排版由编辑器负责，IDEA 自动使用版本化的 `.idea/codeStyles` 项目方案和 `.editorconfig`；根 POM 仅提供 Checkstyle 配置，不自动修改源码，也不绑定普通 Maven 生命周期。
- `.github/workflows/verification.yml` 直接检查分支中变更的 Java 文件。`mvn -N checkstyle:check` 可用于本地全仓审计，存量告警清零后再改为 CI 全仓门禁。
- `mvn test` 由 Surefire 执行 `*Test` 单元测试，不运行 `*IT`。
- `mvn verify` 先执行单元测试，再由 Failsafe 执行 `*IT` 集成测试，并完成打包验证。
- 根 `deploy` profile 默认设置 `skipTests=true`，主要用于 flatten、源码/Javadoc 和签名/发布相关构建。
- 本地验证 deploy 构建时使用 `-Dgpg.skip=true`，避免要求本地签名环境。
- 修改 CI 时保持 JDK 17，并显式声明集成测试所需服务；当前 `.github/workflows/verification.yml` 提供 Redis 7 并执行 `mvn verify`。

## 发布安全

- 根目录 `release.sh` 会切换分支、拉取远端、提交、打 tag、推送，并在最后重置和清理工作区，具有破坏性。
- 除非用户明确要求且工作区状态已核对，否则不要执行 `release.sh`。
- 发布前确认版本号、目标分支、凭据、远端和工作区干净状态；发布后报告生成的 commit/tag 与推送结果。
