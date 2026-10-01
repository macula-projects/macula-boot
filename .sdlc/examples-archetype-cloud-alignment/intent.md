# Intent: Examples 与 Archetype 云端部署规范对齐
Author: Rain. Status: accepted.

## Problem
当前 `macula-boot-examples` 与 `macula-boot-archetype` 尚未形成一致、完整的本地开发部署体验。示例工程虽然已经提供 Alibaba 和 Tencent 两套链路及 Docker Compose 环境，但其部署入口、环境配置和初始化方式与 `macula-cloud/deploy` 的实践不统一；通过 Archetype 生成的新项目也不能仅完成环境配置后，就同时支持基础设施容器、IDE 运行应用和完整容器运行。生成项目中的 Admin 登录实现还没有同步 `macula-cloud-admin` 已完成的登录修复。

## Proposed outcome
开发者能够在保留 Alibaba 和 Tencent 两套示例链路的前提下，使用统一的部署规范运行 `macula-boot-examples`。通过 Archetype 生成的新项目自带完整的本地开发部署能力，配置 `.env` 后即可选择“基础设施容器 + 本机 IDE 应用”或完整容器模式启动，并能够登录 Admin、通过网关完成一条管理接口调用。生成的 Admin 同步 `macula-cloud-admin` 已验证的登录修复，但不引入其完整系统管理功能。

## Affected users and systems
受影响用户包括使用 `macula-boot-examples` 学习和验证框架能力的开发者，以及使用 `macula-boot-archetype` 创建新项目的开发团队。受影响范围包括 Alibaba 和 Tencent 示例链路、示例 Docker Compose 环境、Archetype 生成项目的部署目录与 Dockerfile、后端运行配置、Admin 登录流程、相关 README 和验证流程。`macula-cloud` 仅作为部署规范、Dockerfile 和 Admin 登录修复的参考来源，不作为本次修改目标。

## Constraints
- 保持 Java 17、Spring Boot 4.0 和 Spring Cloud 2025.1 兼容。
- 保留现有 Alibaba 和 Tencent 两套示例链路，不将其替换为 `macula-cloud` 的业务模块。
- Archetype 必须包含完整的本地 `deploy/` 能力，并同时支持基础设施容器加 IDE 应用、完整容器两种运行模式。
- Admin 只借鉴 `macula-cloud` 的部署方式和 Dockerfile，并同步登录修复；不复制租户、用户、角色、菜单、字典、日志、应用管理等完整系统管理功能。
- 生成项目应以配置 `.env` 为主要启动前置条件，不要求开发者修改生成后的源码才能完成验收链路。
- 保留 Archetype 的占位符、多模块结构和可生成性；不得提交真实凭据或生产配置。
- 实际文件修改必须在独立功能分支上进行；任何 SDLC gate 均由人类明确批准。

## Open questions
- 如果跳过本次改造，对开发者、维护者或项目交付造成的具体成本或风险是什么？
- 本次改造是否有明确的交付期限或发布版本要求？
- “一条管理接口调用”的验收接口由后续 Design 阶段基于现有轻量示例确定，还是有指定接口？
