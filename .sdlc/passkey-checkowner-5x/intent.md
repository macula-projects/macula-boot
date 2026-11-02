# Intent: 将 PassKey 与 CheckOwner 资源鉴权回移至 5.x
Author: Rain. Status: accepted.

## Problem
Macula Boot 5.x 的应用运行在 Java 8 和 Spring Boot 2.7.18 技术栈上，目前不能使用主线已经提供的统一 CheckOwner 归属检查与 PassKey 资源访问凭证能力。继续缺少该能力时，5.x 应用仍需各自处理资源编号被替换后的越权访问风险，主线与维护分支的资源鉴权行为也不一致。

## Proposed outcome
5.x 使用者能够获得与主线一致的资源鉴权能力：对可明确判断拥有者的返回结果执行 CheckOwner 后置检查；对必须依靠已授权列表界定访问范围的资源，在业务方法执行前验证绑定当前用户、资源编号和有效期的 PassKey；已完成数据权限过滤的列表能够向客户端返回 PassKey。失败场景应明确拒绝访问，不能静默放行或退化到另一种模式。

## Affected users and systems
受影响对象包括仍使用 Macula Boot 5.x 的业务应用、公共 DTO 契约、Security Starter、Web Starter、列表与详情接口调用方，以及依赖 CheckOwner 或 PassKey 完成资源级授权的前端和服务端代码。

## Constraints
目标分支为 `5.x`，必须兼容 Java 8、Spring Boot 2.7.18 及该分支现有 Spring Security、Jackson、Servlet 和测试基础设施。保持 5.x 现有公开行为和模块边界，不引入仅适用于主线 Java 17/Spring Boot 4 的 API。完整保持主线 PR #43 已确认的外部契约与行为，包括统一 `@CheckOwner`、PassKey Header/Query、列表包装范围、默认关闭及关闭时跳过签发；不得降低主线已经确认的资源鉴权安全语义。完成后创建独立分支、提交、推送，并创建以 `5.x` 为目标分支的 PR；不包含合并、发布或部署。

## Open questions
无。
