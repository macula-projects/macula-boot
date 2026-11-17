# 5.x 验证证据与草稿边界

## 当前有效状态：SQL 重写已按用户要求撤回
- MyDataPermissionHandler 与任务起始基线完全一致；原生 DataPermissionInterceptor 接入恢复，新增参数上下文、参数化插件及 SQL 专用测试删除。ROOT 租户授权、JWT、Token 缓存、请求体 10 MiB、日志清理等其他修复保留。
- 下文所有 SQL 参数化、H2/MySQL、JOIN/CTE 记录均为已撤销实现的历史证据，不代表当前 SQL 漏洞已修复。PR 保持草稿，SQL 部分等待重新评估，不能按旧 Verification is green 进入合并。
- 原实现本轮重新复现：`SELF=create_by = "alice" OR 1 = 1`；`OR=active = 1 OR id = 2 AND dept_id = 12`；`DEPT_SUB` 仍使用 find_in_set。日志 `/tmp/macula-compat-review/OriginalPermissionReevaluation.log`。前两项是需要单独修复的注入与逻辑分组问题；第三项是原 MySQL 方言约束，不再混入安全修复改造。
- Handler SPI 只返回 Expression，没有 BoundSql/ParameterMapping；先评估保留原结构的最小安全改法，不能再默认扩展为 SQL 参数基础设施。此轮只恢复和评估，不新增替代 SQL 实现。


## 状态
全仓验证未通过，不能声明 Verification is green。Rain 在看到实际阻塞后明确选择“先提交标明验证限制的 5.x 草稿 PR”。该授权只允许草稿评审，不批准合并，不代表接受生产风险或放宽测试。

## 已完成
- `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus,macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=SecurityHardeningRegressionTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.compiler.release=8`：10 项新增回归通过，0 failures/errors/skips，日志 `/tmp/macula-security-5x-regression.log`。实际输出为两个测试类分别 Tests run: 4 / 6，BUILD SUCCESS。
- 在隔离 MySQL 8.4.6 设置 MACULA_SECURITY_TEST_MYSQL_URL / USER / PASSWORD 后执行 `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am test-compile org.apache.maven.plugins:maven-failsafe-plugin:3.5.6:integration-test org.apache.maven.plugins:maven-failsafe-plugin:3.5.6:verify -Dit.test=SecurityHardeningDatabaseIT -Dfailsafe.failIfNoSpecifiedTests=false -Dmaven.compiler.release=8`：H2 TOP 与真实 MySQL 两项通过，0 failures/errors/skips；日志 `/tmp/macula-security-5x-database-it.log`。
- 字节码检查：DataPermissionParameterContext `major version: 52`，保留 Java 8；使用 release=8 编译避免误用 JDK 17 API。原 Boot 2.7、MyBatis-Plus 3.5.7、JSqlParser 4.9 与 javax 不变，没有新增生产依赖。
- 主线 Checkstyle 规则对本次 22 个变更 Java 文件执行检查；详细结果见最终构建记录。git diff --check 通过。

## 全仓阻塞及基线对照
执行 `mvn -fae verify -Dmaven.compiler.release=8 -Dspring.kafka.bootstrap-servers=127.0.0.1:32909 -Dspring.kafka.producer.properties.max.block.ms=1000 -Drocketmq.name-server=127.0.0.1:32987`，失败日志 `/tmp/macula-security-5x-full-verify.log`。Kafka/RocketMQ 显式指向未使用的测试端口，防止读取本机业务服务；没有为了通过而禁用测试。

- DateTimeTest：`java.text.ParseException: Unparseable date: "2019-02-26"`。在 origin/5.x 原始基线 c39821cadce3a6a8086585e43c820654cb83a888 上使用同一测试复现，日志 `/tmp/macula-security-5x-baseline-date.log`。
- RedissonOneRedisTest / TwoRedisTest：`NoSuchMethodError: org.yaml.snakeyaml.parser.ParserImpl.<init>(StreamReader, LoaderOptions)`；原始 5.x 基线 RedissonOneRedisTest 同样复现，日志 `/tmp/macula-security-5x-baseline-redis.log`。属于旧依赖 API 不匹配，Redis 本身为临时隔离容器且正常 PONG。
- TinyID / Kafka：旧测试需要未提供的独立服务；没有访问已有业务服务，也没有把服务缺失视为修复代码失败。原 root -fae 因前述模块失败跳过依赖它们的后续模块，因此不宣称全仓覆盖完整。

## 兼容范围
JSqlParser 4.9 的 TOP 没有 visit(Top) 扩展点，按实际输出次序先记录 TOP 原参数，再保留默认 TOP 输出；Oracle 层级 visitor 保留 PRIOR/NOCYCLE/子句顺序。部门仍为同 SQL 子查询，无额外 JDBC 部门查询。

5.x 不在 Starter 打包 jose.jks；签名资源仍由应用提供，保持原默认资源/密码约定。测试通过临时生成的 JKS 验证自定义 encoder 依赖默认 source，不新增生产私钥。请求体保持原分支及链构造时机，默认 10 MiB；派生 JWT 使用简单 id:/u: 且保留 5.x a 租户段。未搬入 6.x API Key / IAM 协议。

受保护配置和依赖版本未修改，无适用 eval。草稿必须保留以上限制，完成旧测试/依赖修复前不转为可合并。

## 恢复后实际验证
- 5.x clean test：ROOT 1 与 Gateway 6，共 7 项通过；日志 /tmp/macula-original-handler-restore-5x.log，保留 Java 8 release=8。
- 两分支 Handler 均与各自任务起始基线逐字节一致；新增 SQL 类与引用已移除。当前变更文件 Checkstyle 零违规，git diff --check 通过。没有运行新的 SQL 修复验证，因为当前没有 SQL 修复实现。
