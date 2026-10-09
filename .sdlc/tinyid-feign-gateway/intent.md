# Intent: TinyID 号段客户端统一网关接入
Author: 待确认. Status: accepted.

## Problem
TinyID starter 当前通过独立 HTTP 实现访问服务端，维护专用服务地址、超时和 token 配置，与 system starter 的统一网关接入方式不一致，增加客户端实现与接入配置的维护成本。

## Proposed outcome
TinyID starter 申请号段时通过统一网关访问 Server，调用方只提供 bizType，不再配置或传递 TinyID 专用 token。接入配置与 system starter 保持一致，服务端及网关同步支持该调用方式。

## Affected users and systems
使用 TinyID starter 的应用；Macula Boot TinyID starter；Macula Cloud TinyID Server 及网关；相关配置示例、README 和测试。

## Constraints
- 按用户要求定义 Feign 接口，替换 HttpSegmentIdServiceImpl 的服务端访问方式，参照 system starter。
- 保留客户端号段缓存、预加载、本地发号及现有发号算法和公共调用方式。
- 移除本次号段调用链路的 TinyID 专用 token，沿用统一网关认证配置；bizType 是申请号段的业务参数。
- 同步调整客户端、服务端与网关配置以及相关文档和测试。
- 保持 Java 17 兼容，保留工作区已有改动。
- 本次不主动扩展到应用管理、授权管理页面或数据库表清理。

## Open questions
- 需求发起人的署名待确认。
- 旧版 token 接口是否需要兼容，以及其他直接取号接口是否同步迁移，需在设计前明确。
- 现有应用与 bizType 授权关系在统一网关认证下如何处理，需在设计阶段明确；移除专用 token 参数本身不定义该关系的去留。
