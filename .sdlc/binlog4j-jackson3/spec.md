# Spec: Binlog4j 使用 Jackson 3 (from intent.md 2026-10-08)
Status: accepted

## Source intent
[intent.md](intent.md)。接受依据：用户对对话中完整 Jackson 3 方案的明确确认。

## Requirements
1. Starter 显式依赖受父级管理的 Jackson 3 databind，移除 fastjson2 直接依赖。
2. 独立实体 Mapper 使用 SNAKE_CASE 和 convertValue，忽略无对应实体属性的列；保留 Map 返回路径和既有日期偏移。
3. 独立位点 Mapper 保留 serverId、position、filename、gtidSet，读取历史 JSON。
4. 示例和文档移除 fastjson2 API 与注解，说明 JsonProperty 定制映射。
5. 用不连接外部服务的测试验证类型转换、异常及位点兼容。

## Non-goals
不调整父 POM 版本管理、事件调度、连接管理、Redis Codec、公共接口或应用全局 Mapper。

## Design
实体和位点各自复用静态不可变 JsonMapper；实体使用 SNAKE_CASE，位点保留默认驼峰命名。

## Data and interfaces
公共 Java 接口和 Redis key 不变。业务实体的 fastjson 注解不再生效，特殊名称使用 Jackson JsonProperty。

## Flagged concerns
- 类型转换并非 fastjson 的完整等价实现：覆盖日期、数字、布尔、枚举、二进制和错误输入；下游自定义反序列化器需迁移，non-blocking。
- 位点 JSON 文本顺序及 null 输出可能不同，以字段值兼容为准，non-blocking。

## Verification strategy
新增实体转换与 Redis mock 回归测试；目标 reactor test、全仓单元测试、依赖树和改动 Java Checkstyle 检查。实际服务联调不作为本次单元验证的结论。
