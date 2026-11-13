# Intent: Alibaba Gateway 路径变量加解密示例
Author: 待确认. Status: accepted.

## Problem
用户在排查 CryptoGlobalFilter 对包含路径变量的请求的处理行为。目前 Alibaba Gateway 示例已有本地 CryptoService 实现，但缺少可直接执行并核对结果的完整加解密示例，无法方便地验证路径变量保留在 URL、其他参数加密传输时的实际行为。

## Proposed outcome
在 macula-example-alibaba-gateway 提供可复现的加解密示例与调用说明，覆盖明文路径变量、加密 Query 参数、加密 JSON Body 和响应数据解密。执行者能够核对路径变量及解密参数是否正确到达处理端，并确认响应能够还原。

## Affected users and systems
使用 Macula Gateway 加解密能力的开发者，以及 macula-example-alibaba-gateway 示例应用和示例调用端。

## Constraints
路径变量保留在 URL 中，不放入加密体。沿用已有 SM2/SM4 实现及 CryptoGlobalFilter 协议，遵守 Java 17 和现有模块边界。示例说明使用中文，明确启动依赖和实际验证范围。若验证发现框架缺陷或需要扩展到其他模块，单独说明具体问题与修改范围。

## Open questions
- 示例是否需要同时演示现有签名校验，还是仅演示加解密？
- intent 作者署名待确认。
