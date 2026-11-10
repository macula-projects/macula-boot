# Review findings: application-url-ip-allowlist

## Plan
Reviewer: sdlc-reviewer 独立子代理（codex-plan-review 未安装时的技能规定 fallback）。
Budget: 仅 Plan pass，最多三轮；首轮后仅评审变更，不重复已记录问题，同类只报一次。

### Round 1
- `.sdlc/application-url-ip-allowlist/plan.md:7` — Important — 子标题使 artifact-gate 提前结束清单，别名及多文件同一行不能可靠匹配 — 已改为两仓各自每行一个完整仓库相对路径，无子标题、别名及附注。
- `.sdlc/application-url-ip-allowlist/plan.md:47` — Important — hook 仅承认存在于 HEAD 的 plan，“不自动提交”会导致实现前准入失败 — 已将仅本地提交本次已接受 SDLC 工件写入待人工批准的 Order 1，不提交源码或进行远端操作；未批准时不绕过。
- hook 验证是只读调用，exit 2，未尝试实际源码写入。此为插件规则验证，不声称实际工具写入已被拦截。
- 其余 accepted spec 范围、继承、字段省略/清空、共享校验、mapped CIDR、缓存和隔离验证未发现额外阻塞。

### Round 2
独立复查两仓完整路径清单与 Order 1 修订：第 1 轮两项 Important 均已修正，未发现新增问题。实际共执行 2 轮 Plan 评审，未执行代码审查或业务测试。计划仍为 draft，等待用户明确批准。

## Code Round 1 — becdfab0a46bc90b1d65fa8211cd2daebbc44ca8
Reviewer: boot_code_review. Depth: Standard. Code round cap: 3.
Passes run: Bugs, Security, Compliance. Scope: origin/main...becdfab0a46bc90b1d65fa8211cd2daebbc44ca8.

- macula-boot-commons/src/main/java/dev/macula/boot/security/AccessRuleUtils.java:159 — Minor — BC IPv6 boolean 接受非法尾部单冒号并拒绝合法末尾单零组压缩 — 独立分组/压缩结构校验并补测试 — Response: fixed（待修复提交复查）。
- 独立 fresh-context verifier code_finding_verifier: real；使用当前 target/classes 和实际依赖复现非法地址被接受并匹配、合法地址被拒绝。Verifier rejected: 0。
- Security：无其他发现。Compliance：无适用规则违反。Pre-existing：0。Suppressed duplicates：0。Nits：0。

## Recurring
已扫描两仓其他 review-findings.md，未确认本次 IPv6 依赖校验边界误判类别在两个独立变更出现。当前实例由 AccessRuleUtilsTest 的压缩位置/词数/尾段回归约束，不新增无来源的合规规则。
