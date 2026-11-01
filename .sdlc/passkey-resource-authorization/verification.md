# Verification: 统一 CheckOwner 资源鉴权

日期：2026-11-01。分支：codex/passkey-resource-authorization。
用户确认：“整体上代码我审查后是OK的”。下方为前一版本验证记录。用户随后明确关闭开关应跳过列表签发，已按用户要求修改条件装配及 Advice；本次增量验证已完成，最新结果见文末，旧结果仅保留为历史记录。

## 实际执行

| 命令 | 结果 |
| --- | --- |
| `mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am test -Dtest=PasskeyResourcesTest,ResultModelJsonTest,PasskeyServiceTest,PasskeyRequestResolverTest,CheckOwnerTest,SecurityAutoConfigurationTest,PasskeyMvcTest,ControllerResponseAdviceTest,WebAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false` | 48 个测试，失败/错误/跳过均 0 |
| `mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am test` | 85 个测试，失败/错误/跳过均 0 |
| `mvn test` | 全仓 256 个测试，失败/错误/跳过均 0 |
| `mvn clean verify` | BUILD SUCCESS；Surefire 256 个测试，失败/错误/跳过均 0；Failsafe 50 个测试，失败/错误均 0，跳过 3，实际执行 47 |
| `mvn -N checkstyle:check -Dcheckstyle.includes=<全部变更及新增 Java 文件的精确列表>` | 0 violations，BUILD SUCCESS |
| `git diff --check` | exit 0，无输出 |

清理构建使用 Java 17，并访问已确认可用的本机 Redis（PONG）。没有启动、重建或修改外部服务配置。Failsafe 的 3 个跳过是仓库既有条件：Kafka、RocketMQ 测试带 Disabled；TinyID 真实服务测试要求 TINYID_E2E=true。本次未验证这三个真实服务。

## 实际输出摘录

定向测试的三个模块汇总：
```text
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

全仓 test：
```text
[INFO] BUILD SUCCESS
[INFO] Total time:  02:07 min
[INFO] Finished at: 2026-11-01T22:01:06+08:00
```

全仓 clean verify：
```text
[INFO] BUILD SUCCESS
[INFO] Total time:  01:25 min
[INFO] Finished at: 2026-11-01T22:02:53+08:00
```

从 clean verify 后的 TEST-*.xml 聚合（依次为 tests/failures/errors/skipped）：
```text
surefire 68 [256, 0, 0, 0]
failsafe 18 [50, 0, 0, 3]
```

Checkstyle：
```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

原始命令日志：/tmp/passkey-focused-test.log、/tmp/passkey-module-test.log、/tmp/passkey-all-test.log、/tmp/passkey-clean-verify.log、/tmp/passkey-checkstyle.log。逐项报告在各模块 target/surefire-reports 和 target/failsafe-reports。

## Proof 与设计验证映射

- 公共契约/JSON：PasskeyResourcesTest、ResultModelJsonTest 通过，覆盖支持的集合及包装、混合元素、空列表、单对象不签发、循环保护与隐藏辅助编号。
- HMAC/有效期：PasskeyServiceTest 通过，固定 Clock 验证身份与编号绑定、篡改、到期精确边界、非法配置/格式/长度、字段编码边界和并发使用。
- 凭证来源：PasskeyRequestResolverTest 通过，覆盖 Header/URL Query、同值双来源、重复/冲突/编码错误、表单不参与及缺失请求。
- 模式与代理：CheckOwnerTest 15 个用例通过，回归原归属检查，新增位置/命名/嵌套参数、混合及字面量拒绝、接口/JDK/泛型桥接、无请求/禁用拒绝和 PMS 组合。拒绝的前置场景断言方法计数不增加。
- 正式自动配置：SecurityAutoConfigurationTest 通过，验证后置兼容、默认禁用、合法/非法启用配置、服务/Clock 替换；额外通过正式装配的 Advisor 验证有效编号允许、换编号和关闭时拒绝。
- 实际 MVC 组合：PasskeyMvcTest 通过，真实签发服务、Advisor、两个 Advice 联合验证列表凭证到不同详情 DTO、PathVariable、RequestBody、Header/Query、写计数、403/A0312。后置拒绝允许只读方法执行但不向调用者返回详情，普通异常保持 500。
- Web 兼容：ControllerResponseAdviceTest、WebAutoConfigurationTest 通过，覆盖包装、分页、混合、单对象、无签发器拒绝、Feign/跳过包装时仍处理签发以及 Advice 条件装配。
- 跨模块：目标模块、全仓单元测试、clean verify 全部成功；真实 Redis 集成测试已执行。未宣称 Kafka/RocketMQ/TinyID 真实服务验证完成。

## 失败及修复

首轮定向测试有 16 个失败：CheckOwnerTest 和 PasskeyMvcTest 的手工上下文未把 Advisor 标为基础设施，Spring 方法安全代理忽略该 Bean，导致本应拒绝的调用直接执行。正式 SecurityAutoConfiguration 原有 ROLE_INFRASTRUCTURE。修复两个测试上下文与正式配置一致，补充正式配置前置用例；随后定向、模块、全仓和清理集成构建均通过。无生产代码变动。

## 配置与 eval

变更包含应用自动配置和属性，不涉及 CLAUDE.md、skill、hook、REVIEW.md 或 reviewer 定义，未触发对应配置 eval；未修改或弱化审查策略。

此前版本 Verification is green，范围为当时设计的测试及仓库启用的测试；三个既有可选外部服务测试按其条件跳过。该结果不替代业务列表过滤正确性验证，也不证明真实应用部署或跨用户共享 DTO 缓存的安全性。

## 开关语义调整后的最终验证

用户进一步明确 enabled=false 时列表无需 issue，已修订设计与实现：默认 HMAC 服务、默认 Clock 仅 enabled=true 装配；既有 Advice 在关闭时完全跳过列表遍历与签发，即使有自定义 issuer 也不调用。列表正常返回；统一 Advisor 保留，前置 Passkey 模式仍拒绝，后置归属模式继续检查。

新增验证：默认未配置/显式 false/true 的实际 Web 自动配置属性绑定；关闭时存在/不存在自定义签发器的列表与包装；启用但缺签发器拒绝；默认/显式关闭时无默认服务和 Clock，非法签名配置不再触发服务创建，后置检查继续生效、前置仍拒绝。

实际执行：
- `mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am test`：88 个测试，失败/错误/跳过均 0。
- `mvn test`：259 个测试，失败/错误/跳过均 0。
- `mvn clean verify`：Surefire 259 个测试，失败/错误/跳过均 0；Failsafe 50 个测试，失败/错误均 0，跳过 3，实际执行 47。跳过原因与上文一致。
- 全部变更 Java 精确范围 Checkstyle：0 violations；git diff --check：exit 0。

实际 clean verify 输出：
```text
[INFO] BUILD SUCCESS
[INFO] Total time:  01:24 min
[INFO] Finished at: 2026-11-01T22:20:09+08:00
```

实际 Checkstyle 输出：
```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

最新原始日志：/tmp/passkey-switch-module-test.log、/tmp/passkey-switch-all-test.log、/tmp/passkey-switch-clean-verify.log、/tmp/passkey-switch-checkstyle.log。无 protected review 配置变化，无新增对应 eval 要求。

Verification is green。上述结果验证最新开关语义；不将既有外部服务跳过项或真实应用部署宣称为已验证。

## 代码审查修复后的最终验证
独立复核确认类代理继承接口命名参数表达式时误拒绝。新增泛型接口/JDK/CGLIB 回归先运行失败：`Tests run: 1, Failures: 0, Errors: 1, Skipped: 0`，CheckOwnerAccessDeniedException，日志 /tmp/passkey-proxy-red.log。已改为按实际注解声明方法解析参数并建立 SpEL 上下文，本次修复后的完整验证已完成，结果如下；上文结果保留为修复前证据。

- `mvn -pl macula-boot-starters/macula-boot-starter-security -am test -Dtest=CheckOwnerTest -Dsurefire.failIfNoSpecifiedTests=false`：16 个测试，失败/错误/跳过均 0，覆盖 JDK/CGLIB 的泛型接口声明参数名与实现不同、有效与错误编号及方法计数。
- `mvn test`：260 个测试，失败/错误/跳过均 0。
- `mvn clean verify`：Surefire 260/0/0/0，Failsafe 50/0/0/3（47 个实际执行）；跳过条件与此前一致。
- 变更 Java 精确范围 Checkstyle：0 violations；git diff --check：exit 0。

实际输出：
```text
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
```text
[INFO] BUILD SUCCESS
[INFO] Total time:  01:25 min
[INFO] Finished at: 2026-11-01T22:45:11+08:00
```

最新日志：/tmp/passkey-proxy-red.log、/tmp/passkey-proxy-green.log、/tmp/passkey-review-all-test.log、/tmp/passkey-review-clean-verify.log、/tmp/passkey-review-checkstyle.log。

Verification is green。审查修复未修改 review/skill/hook 等受保护配置，无新增对应 eval。
