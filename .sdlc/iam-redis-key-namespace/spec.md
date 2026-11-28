# Spec: 统一 IAM Redis Key 命名（源自 intent.md，2026-11-28）
Status: accepted

## Source intent

[已确认的 IAM Redis Key 命名意图](intent.md)。用户确认系统尚未上线，直接切换。

## Requirements

1. IAM 的 Redis Key 统一使用 `macula:cloud:iam:`。IAM 与 System 对同一共享键产生一致名称。
2. 新版直接使用新前缀。旧命名下的测试状态不迁移，已有 TTL 自然生效。仍使用旧围栏的测试令牌按现有判定拒绝，可重新登录生成新状态。
3. 保留 Redis 键的数据类型、后缀、TTL 规则和 Cluster 哈希标签。缺失、损坏或过期状态继续按照各自原有安全规则处理。
4. 现有密文继续可解密。已使用正确命名的 Redis 键以及数据库中的授权记录定位继续有效。

## Non-goals

OAuth 协议、令牌格式、会话期限、Redis 值格式、加密上下文和浏览器会话属性沿用现有契约。镜像更新和环境操作独立授权。

## Design

- **统一键名**。在 Boot Commons 定义 IAM Redis 前缀。核对实际写入和读取的键，覆盖 IAM、System、Lua、配置与测试中的旧前缀。IAM 与 System 的人机验证就绪、密码到期链接和令牌发布围栏使用同一命名。将只发现声明的旧 OAuth2 Client 常量也改为统一前缀。
- **直接切换**。新版读写规范键。旧键保留在各自原 TTL 下，业务读取转到新命名。测试环境如遇使用旧状态的会话、MFA 或绑定流程，可重新发起。
- **授权记录和协议标识**。OAuth2 授权记录及索引已采用目标前缀。保持记录定位、摘要及 Cluster 哈希标签，更新围栏命名。继续拒绝缺失围栏或记录不匹配的令牌。Redis 以外的标识和已有密文的用途标识保持兼容。

## Data and interfaces

| 边界 | 契约 |
| --- | --- |
| Redis Key | `iam:<suffix>` 和 `cloud:iam:<suffix>` 中确属 IAM Redis 的键，统一为 `macula:cloud:iam:<suffix>`。 |
| 共享状态 | IAM 写入、System 或 IAM 读取的键在同版切换，使用共享前缀及稳定后缀。 |
| 数据库 | 授权发布及清理表的 `record_locator` 继续使用已规范命名的授权记录定位。 |

## Flagged concerns

- 未上线环境中的在途测试 [non-blocking]。测试会话、MFA 或绑定票据如果仍依赖旧键，切换后需要重新发起。现存旧键按 TTL 过期。
- 跨服务同步 [non-blocking]。IAM 和 System 的共享就绪状态及令牌发布围栏按统一前缀一次切换。验证要覆盖两端读取、已发布令牌的拒绝路径以及新签发令牌。

## Verification strategy

- 按键族核对最终前缀、共享读写、后缀、TTL 与 Lua Cluster 哈希标签。
- 在隔离 Redis 与 IAM/System 测试检查新会话、MFA、验证码、限流、防重放、发布及撤销。校验旧围栏无法使无效令牌通过。
- 验证现有密文解密与已有规范授权记录定位，检查差异和日志。依据 Boot、Cloud 的 AGENTS.md、Cloud 架构规则及 Boot REVIEW.md 执行。
