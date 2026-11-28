# Spec: 校正 MyBatis-Plus 基类审计字段说明 (from intent.md 2026-11-28)
Status: accepted

## Source intent

[已确认的意图](intent.md)

## Requirements

1. `BaseEntity.createTime` 生成的接口描述为“创建时间”，`BaseEntity.lastUpdateBy` 为“更新人”。
2. 保留四个属性的名称、类型和 MyBatis-Plus 自动填充注解，使用该基类的调用方继续按现有字段契约运行。
3. Starter README 中复制的 `BaseEntity` 示例与实际源码描述一致。

## Non-goals

ORM 映射、自动填充行为和 Boot 配置属性沿用当前契约。Cloud IAM 表结构在其仓库独立设计。

## Design

校正 `BaseEntity` 和同模块 README 示例中这两条 `@Schema` 描述。README 当前的审计配置表已正确。设计受仓库 `AGENTS.md`、`REVIEW.md` 与测试规则约束。

## Data and interfaces

接口模型生成器读取两条更正的描述。序列化字段、类型和数据库访问约定沿用现有定义。

## Flagged concerns

- README 示例重复了同一处错误: 设计包含同步修正示例的两行文字，避免源码与文档示例不一致，non-blocking。

## Verification strategy

比较四个审计字段的注解及类型，检查差异仅属于两条描述及对应 README 示例。运行 MyBatis-Plus Starter 相关测试确认现有行为。
