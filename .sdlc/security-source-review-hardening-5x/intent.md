# Intent: 5.x 安全修复回移

当前范围更正：Rain 要求恢复原 MyDataPermissionHandler 并重新评估；SQL 注入/OR 修复目标尚未完成，参数框架已撤销。保留其他已接受修复，PR 维持草稿，不代表接受原 SQL 风险。

Author: Rain. Status: accepted.

Rain 明确要求主线全面审查并提交 PR，同时为 5.x 修复。回移主线已接受的 SQL 参数化与分组、ROOT 租户切换、JWT 配置与 Secret、Token 到期、10 MiB 请求体和敏感日志、id:/u: 缓存分类。保留 5.x 独立的认证协议、javax、依赖版本、Java 8 字节码兼容和原过滤器控制流；不合并发布或操作业务数据。
