# Intent: 统一 IAM Redis Key 命名
Author: Rain. Status: accepted.

## Problem

IAM 的 Redis Key 同时使用 `iam:`、`cloud:iam:` 和 `macula:cloud:iam:`。部分键由 IAM 和 System 共同读写，前缀不一致会增加查找和维护成本。改名时若两个服务使用不同前缀，或遗漏已经记录的键引用，仍有效的认证和安全状态可能无法读取。

## Proposed outcome

IAM 拥有的 Redis Key 统一使用 `macula:cloud:iam:`。IAM 与 System 对共享键使用一致的命名。本系统尚未上线，直接启用新前缀。切换后重新建立此前依赖旧键的测试状态。

## Affected users and systems

Macula Cloud IAM、System、System Redis，以及 Boot Commons 中实际由 IAM 使用的共享常量。涉及 MFA、验证码与限流、外部绑定、会话和令牌安全状态、第三方目录任务及共享状态。通过源码和运行态只读清单核对实际生产者与消费者。

## Constraints

- 本次范围为 IAM 的 Redis 键名。已有密文的加密上下文、浏览器会话属性和其他用途的标识遵循各自现有契约。
- 保留 Redis Cluster 哈希标签、键的数据类型、过期语义和安全状态的严格性。持久化键引用按现有授权契约校验，缺失状态默认拒绝。
- 按 Boot 与 Cloud 的发布边界协调共享常量与多服务读写。

## Open questions

切换策略已确认。系统未上线，直接启用新命名，旧前缀存量无需迁移。
