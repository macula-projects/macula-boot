# 架构与模块边界

仅在理解仓库结构或进行跨模块修改时加载。

## 顶层模块

- `macula-boot-parent`：依赖版本与 Maven 插件管理，是所有模块的版本基线。
- `macula-boot-commons`：低层公共 API，包括结果模型、异常、上下文和常量；避免引入具体 Starter 依赖。
- `macula-boot-starters`：按能力拆分的 Spring Boot Starter；云能力位于其 `macula-boot-starter-cloud` 聚合模块。
- `macula-boot-examples`：可运行集成示例，不应成为框架模块的依赖来源。
- `macula-boot-archetype`：项目骨架模板；模板内的占位符和多模块结构必须保留。

## 修改原则

- 通用且无框架耦合的能力放入 `commons`；具体集成留在对应 Starter。
- 不让一个 Starter 偶然依赖另一个 Starter 的内部实现；跨模块复用先确认公共 API 边界。
- 云厂商特有逻辑留在 Alibaba/Tencent 对应模块；共享抽象放到 cloud 公共模块。
- 修改 archetype 时，在 `src/main/resources/archetype-resources` 中核对生成后的路径、占位符和父子 POM。
- 公共 API 变更应先搜索仓库内所有调用方，并考虑下游兼容性。
