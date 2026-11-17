# Review findings: 5.x 安全回移

## 当前状态：数据权限重写撤回
- 独立审查曾发现参数反解析遗漏 UPDATE JOIN/CTE/FROM 的新增兼容性回归；Rain 随后要求恢复原 Handler，当前已删除引入该问题的参数框架，未采用后续词法改写方案。
- 原 SQL 注入、OR 分组问题仍存在，用户要求先恢复后重新评估，不等同永久接受风险。PR 保持草稿，不自批准合并。其他修复继续沿用已有审查与验证边界。

## Stage: PR review — Round 2, restore delta
- 仅审撤回差异：独立 reviewer 确认 Handler 与任务基线一致，原生拦截器和顺序恢复，ROOT 限制保留，无新源码缺陷、无孤立引用。
- 上一轮参数框架 Important 因整体撤回已不适用；不是证明原 SQL 注入已修复。5.x 文档曾有一个 Minor（spec 仍称参数方案有效），已重写当前 spec/plan 明确撤回。
- fresh verifier 只核实上述两项：参数框架问题已随撤回消除，文档矛盾已修正；2 项已处置、0 false-positive rejection、0 新发现。
- 原 SELF 拼接与 OR 分组仍属待重评估的 Important 安全问题，PR 不得据此转 ready。当前草稿不批准合并，不接受永久风险。
- 保留的当前范围已做定向回归与规范检查，历史全仓或 SQL 专用结果不能当作恢复后全仓验证。
