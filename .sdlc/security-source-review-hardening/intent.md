# Intent: 最小数据权限修复与租户、网关安全加固
Author: Rain. Status: accepted.

## Problem
SELF 直接拼接 username，原 WHERE 含 OR 时权限条件未分组；请求租户、Token 缓存、默认 Secret 和敏感日志也有已确认问题。

## Proposed outcome
Rain 明确 username 正常仅包含大小写字母、数字、下划线。保留原 MyDataPermissionHandler 与原生插件，只完整校验用户名白名单，非法身份无匹配而非清洗，合法值用单引号；原条件与权限条件分别分组。部门/find_in_set 保持原查询，不新增参数框架或 JDBC 查询。

保留已接受的 ROOT 租户切换、上下文清理、可配置 JWT 来源与独立 Bean 条件、显式 Secret 验证、opaque 缓存剩余 TTL、10 MiB 原流程上限、敏感日志清理和 id:/u: 分类。

## Constraints
不改变 DEFAULT/缺失 dataScope、@Inner、invoker；默认签名材料继续兼容。用户名字符约定若变化须重新评估，不可仅放宽白名单后继续拼接。不修改业务数据、轮换实际密钥、合并或部署。主线及 5.x 通过各自 PR 人工评审；5.x 按 Rain 明确选择保持草稿并列出旧测试阻塞。
