# Review findings: 源码安全审查问题修复

## Stage: PR review — Round 1 (2026-11-17)
- 基线 SHA：0262888965d156148d7d8830e40f994544b4ef89；范围为当前 codex/security-source-hardening 全部实现、测试和相关文档，含新增文件；首次 Stage 5 完整审查。历史三轮为 Plan pass，不计为 Stage 5 轮次。
- 按 REVIEW.md Standard 深度执行 Bugs、Security、Compliance 三个独立 pass。sdlc-reviewer 行为由 security_main_review 子代理只读执行；security_main_verify 使用无上下文的新会话验证 surviving findings，不具备接受风险或批准 gate 的权限。
- Bugs：无新增发现。参数映射在 MyBatis 缓存键生成前同步、保留 additionalParameters 与上下文恢复；TOP/层级保留语义；source/encoder 独立退让；请求体原流程保持。
- Security：无新增未解决发现。SQL 值绑定与逻辑分组、已认证非匿名 ROOT、exp 校验、Redis 调度、id:/u: 分类和统一撤销入口均有证据。
- Compliance：无新增发现；没有额外适用监管要求，没有改变受保护配置、新增依赖或真实凭据。
- 新发布发现 0（Important 0 / Minor 0 / Nit 0 / 新增 Pre-existing 0）；重复抑制 1（已记录的 CryptoGlobalFilter 自定义服务异常文本回传条件风险）；verifier-rejected 0，空列表无待验证项。
- 已接受设计仍在 PR 单列：公开内置默认签名材料、@Inner、invoker、DEFAULT/缺失 dataScope；不同应用使用独立网关缓存为用户明确的部署前提，不由代码测试证明。原 Crypto 异常文本回传条件风险没有被声明为默认私钥泄露，后续处置由人工评审决定。
- 实际全仓 mvn -fae verify：439 项，0 failures/errors，3 项原有条件 skip；真实 MySQL 与 Redis 回调已执行。32 个变更 Java 文件 Checkstyle 零违规，git diff --check 通过。Verification is green。
- 本记录不代表人工批准合并或部署；用户只授权创建 PR，提交后等待人工评审。

## Recurring
- 认证缓存身份隔离与可信来源在应用准入、API Key 对齐及本次审查均涉及。编码规则：AGENTS.md 的可信身份来源/缓存隔离关键逻辑注释约束，以及 REVIEW.md Security pass 对 trust boundary、授权和敏感日志的强制检查；本次以跨类型 key 碰撞回归固定具体行为。

## Stage: Plan

### Round 1
- 基线：HEAD 02628889；审查对象为本轮未提交的 intent.md、spec.md、plan.md，使用本地工作区快照；不存在先前审查轮次。
- REVIEW.md 未指定 Stage 3 reviewer；默认 codex-plan-review 未安装，按 sdlc-build 要求回退到 sdlc-reviewer 子代理行为。插件声明的 Claude 模型在本会话不可用，子代理继承当前可用模型，只执行相同的只读 Plan pass。
- 调用预算：仅 Plan pass，最多三轮；本轮只执行第一轮，后续只看变更、禁止重复发现、同类缺陷合并报告。
- 实际轮次：1。Passes run: Plan。No findings。重复发现抑制数：0。
- 审查结果：spec 1–10 均有工作项和验证覆盖；文件清单包括实现、测试、文档和三套网关配置；保留旧 JWT 默认材料，并单独说明资源服务器 Secret 的迁移。
- 未执行 Bugs/Security/Compliance 代码审查，未运行源码测试，未批准人工 gate。
- Rain 已在会话回复 YES 接受 plan.md，允许按计划开始源码修改；未授权提交、推送或部署。

## Stage: Build
- 已按接受的计划完成源码、测试与文档；本阶段没有委派实现或运行 Stage 5 代码审查。
- 实际偏离和检查结果记录在 plan.md 的 Implementation notes、Build result。
- 等待 Rain 确认实现完整性后进入 sdlc-test；尚未自批准后续 gate。

### Plan Round 2: portability amendment
- 用户明确要求修正 CONVERT 和 find_in_set 的数据库依赖；仅审本次补充及新增文件，原已审部分不重复。
- 沿用 sdlc-reviewer 回退，预算最多三轮；实际本次 1 轮，累计 Plan 2 轮。Passes run: Plan。No findings。重复发现抑制数：0。
- 覆盖参数顺序、完整消费、嵌套/异常上下文恢复、默认策略替换/禁用、H2 与真实数据库验证边界。未批准人工 gate。

## Stage: Build portability amendment
- 用户授权的参数化和可替换部门策略修订已完成；实际结果和实施顺序偏离见 plan.md 的 Portability build result。
- 全仓单元测试 373 通过；H2 IT 在五种兼容模式执行通过，外部 MySQL IT 跳过 1 项。Checkstyle 零违规。
- 未执行 Stage 5 Bugs/Security/Compliance 审查，未自批准后续人工 gate。

### Plan Round 3: user-requested simplification
- 仅审移除额外 JDBC 部门查询/Bean、恢复 SQL 子查询及原工厂入口的补充。Passes run: Plan。No findings。重复抑制 0；累计 Plan 3 轮，达到预算。
- 未批准人工 gate；本轮修正来自 Rain 的明确反馈。

## Stage: Build simplification
- 按 Rain 纠正删除额外 JDBC 部门查询及装配，保留同一 SQL 子查询与 MyBatis 参数绑定；测试与清理重建结果记录在 plan.md 的 Simplified build result。
- 全仓 373 单元测试通过、Checkstyle 零违规、H2 五模式 IT 通过；真实 MySQL IT 跳过。没有自批准 Test 或部署 gate。

## Stage: Build BodyGlobalFilter correction
- 按 Rain 反馈恢复原过滤器控制流和缓存语义，仅保留聚合上限、标准 413 映射和日志清理。
- 相关模块 125 个单元测试通过（包含 8 个 BodyGlobalFilter 回归），Checkstyle 零违规；实际结果记录在 plan.md。没有新增 Plan 审查轮次或批准后续 gate。

## Stage: Test — compatibility and security recheck
- Rain 回复 OK 接受实现并要求复查；使用 sdlc-test 执行新一轮验证。实际命令、输出及需求证据见 verification.md。
- 本轮由主代理分别检查 Bugs、Security、Compliance；不是 Stage 5 独立审查，也没有新增 Plan review 调用或自批准 gate。
- Bugs：Important/P1，新增参数 deparser 对 TOP(?)、Oracle 层级表达式参数处理不完整；当前实现失败而原改写通过，TOP 有真实 H2 执行对照。
- Bugs：Important/P1，新增 JWKSource 条件导致依赖默认 source 的自定义 JwtEncoder 启动失败；相同配置当前失败、HEAD 原类通过。
- Security：Important/P1，原有 JWT 缓存键允许同租户 HMAC 应用 A 使用 tokenId=B 命中 B 的 JWT；真实 A 签名通过，未设置 invoker，最终 JWT sub=B。
- Security：另记录旧异常文本回传的条件风险；没有将它夸大成已确认的默认私钥泄露。保持用户已接受/暂缓策略，不静默扩大修复范围。
- Compliance：本轮没有实现源码变更、依赖新增、凭据变更或远程操作。未发现额外范围偏离；无适用受保护配置 eval。
- 全仓单元测试 375 通过，H2 五模式与真实 HTTP 7 项通过；真实 MySQL、Redis 回调各跳过 1 项。发现仍未修复且真实外部环境证据不足，结论不通过，不能宣布 Verification is green。

## Stage: Build — authorized recheck fixes
- Rain 明确授权统一修复。上述三个 Important/P1 已通过新增回归和原探针对照验证；没有将“测试通过”写成全库安全保证。
- 参数回归先红后绿，补齐 TOP/层级访问并修复默认 deparser 丢弃 PRIOR；缓存 v2 绑定真实验签应用与最终主体；source/encoder 条件独立。
- 全仓单元测试 380 通过，H2 与 HTTP IT 通过；真实 MySQL、Redis 回调各跳过 1 项，规范检查零违规。完整输出与边界在 verification.md。
- 本轮为接受方案内的缺陷修复与构建验证，不是新的 Stage 5 独立审查；没有新增 Plan review 轮次，没有自批准 gate。

## 当前状态：数据权限重写撤回
- 独立审查曾发现参数反解析遗漏 UPDATE JOIN/CTE/FROM 的新增兼容性回归；Rain 随后要求恢复原 Handler，当前已删除引入该问题的参数框架，未采用后续词法改写方案。
- 原 SQL 注入、OR 分组问题仍存在，用户要求先恢复后重新评估，不等同永久接受风险。PR 保持草稿，不自批准合并。其他修复继续沿用已有审查与验证边界。

## Stage: PR review — Round 2, restore delta
- 仅审撤回差异：独立 reviewer 确认 Handler 与任务基线一致，原生拦截器和顺序恢复，ROOT 限制保留，无新源码缺陷、无孤立引用。
- 上一轮参数框架 Important 因整体撤回已不适用；不是证明原 SQL 注入已修复。5.x 文档曾有一个 Minor（spec 仍称参数方案有效），已重写当前 spec/plan 明确撤回。
- fresh verifier 只核实上述两项：参数框架问题已随撤回消除，文档矛盾已修正；2 项已处置、0 false-positive rejection、0 新发现。
- 原 SELF 拼接与 OR 分组仍属待重评估的 Important 安全问题，PR 不得据此转 ready。当前草稿不批准合并，不接受永久风险。
- 保留的当前范围已做定向回归与规范检查，历史全仓或 SQL 专用结果不能当作恢复后全仓验证。
