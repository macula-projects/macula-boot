# Plan: Binlog4j 使用 Jackson 3 (from spec.md 2026-10-08)
Status: accepted

接受依据：用户明确确认对话中的 Jackson 3 实施方案“好，按照这个方案”；本文件记录已确认方案，不新增审批结论。

## Files that change
- macula-boot-starters/macula-boot-starter-binlog4j/pom.xml
- macula-boot-starters/macula-boot-starter-binlog4j/README.md
- 该 Starter 的 src/main/java/dev/macula/boot/starter/binlog4j/BinlogEventHandlerDetails.java
- 该 Starter 的 src/main/java/dev/macula/boot/starter/binlog4j/position/RedisBinlogPositionHandler.java
- 该 Starter 的 src/test/java/dev/macula/boot/starter/binlog4j/test/BinlogEntityMappingTest.java（新增）
- 该 Starter 的 src/test/java/dev/macula/boot/starter/binlog4j/test/BinlogPositionJsonTest.java（新增）
- macula-boot-examples/macula-example-binlog4j/src/main/java/dev/macula/example/binlog4j/entity/User.java
- macula-boot-examples/macula-example-binlog4j/src/main/java/dev/macula/example/binlog4j/handler/UserEventHandler.java

## Order of work
1. 替换依赖及实体、位点 Mapper。
2. 更新示例和 README，注明注解迁移。
3. 增加回归测试并执行相关检查，记录实际结果。

## Risks
类型转换与注解行为可能改变；下游须迁移 fastjson 自定义注解。两个 Mapper 分开，避免历史位点字段变成下划线。父级 fastjson 版本管理保留。

## Proof
实体测试覆盖下划线、JsonProperty、未知列、数字、日期偏移、布尔、枚举、二进制、null、Map 路径和错误转换。位点测试覆盖旧 JSON、空字段、缺失位点及保存后读取。目标及全仓单元测试、依赖树、Checkstyle 提供构建证据。

## Implementation notes
- README 内陈旧的 MySQL connector 和 binlog connector 坐标同步为现有 POM 坐标，不改变依赖版本。
- 项目 Mockito 使用 subclass mock maker，不支持静态 mock；实体测试改为模拟 JDBC DataSource/Connection/ResultSet，临时注入 JDBCUtils 缓存并在 finally 恢复，避免外部连接。
- 错误数字转换实际抛出 Jackson 3 InvalidFormatException；断言采用该实际异常类型。

## Verification evidence
- 2026-10-08：`mvn test -Dstyle.color=never` 全仓 BUILD SUCCESS，包含 binlog4j 示例编译。Binlog4j 共 11 项测试通过，其中新增 8 项，失败、错误、跳过均为 0。
- 改动 Java 文件的 `mvn -N checkstyle:check -Dcheckstyle.includes=...` 通过，0 violations；`git diff --check` 通过。
- `mvn -pl macula-boot-starters/macula-boot-starter-binlog4j -am dependency:tree -Dincludes=com.alibaba.fastjson2:fastjson2,tools.jackson.core:jackson-databind` 显示 Jackson 3.1.5，无 fastjson2。
- 未执行真实 MySQL binlog / Redis 服务联调，未提交、推送或标记 PR/部署门禁通过。
