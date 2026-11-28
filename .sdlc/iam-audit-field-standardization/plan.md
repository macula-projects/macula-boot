# Plan: 校正 MyBatis-Plus 基类审计字段说明 (from spec.md 2026-11-28)

Status: accepted

## Files that change

- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/entity/BaseEntity.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/README.md`
- `AGENTS.md`

## Order of work

1. 在 `BaseEntity` 把 `createTime` 的 Swagger 描述改为“创建时间”，把 `lastUpdateBy` 的描述改为“更新人”。
2. 同步 README 的同一段示例，逐项检查另外两条审计说明、类型、字段名、字段顺序与 MyBatis-Plus 注解。
3. 运行相关 Starter 测试，记录实际测试或环境限制。

## Risks

本项只改变生成接口模型和文档示例的两条说明。数据库定义、持久化、填充后置行为不变。Cloud 的迁移可独立验证，Boot 和 Cloud 分开提交和交付。

## Proof

- 查看补丁，确认业务源码只有两条 `@Schema(description = ...)` 发生变化，README 示例与源码一致。
- `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am test`，记录失败或跳过数，并执行 `git diff --check`。


## 实施调整

用户在实施期间明确要求将标准持久化实体继承 `BaseEntity` 的约定写入 `AGENTS.md`。这是协作规则更新。校正后的源码说明仍限于两条 `@Schema` 描述。
