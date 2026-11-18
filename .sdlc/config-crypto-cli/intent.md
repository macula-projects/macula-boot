# Intent: 配置加解密工具与独立可执行 JAR
Author: Rain. Status: accepted.

## Problem
目前需要为 `CryptoPostProcessor` 准备加密配置值，以及将已有密文还原为明文，但缺少可独立运行的配置加解密工具。

## Proposed outcome
- 提供可调用的工具类，根据给定密钥和明文生成配置密文。
- 支持解密：根据给定密钥和 `mpw:` 密文还原明文。
- 产出可通过 `java -jar` 独立运行的 JAR，无需启动业务应用或连接 Nacos。
- 输出包含 `mpw:` 前缀，可直接用于本地或 Nacos 配置；应用使用同一密钥时，现有 `CryptoPostProcessor` 能正确还原。
- 同时支持命令行参数和交互输入，允许选择加密或解密操作。

## Affected users and systems
使用 Macula Boot 5.x Crypto Starter 配置加密的开发及运维人员。

## Constraints
- 与现有 `CryptoPostProcessor` 的 AES 解密协议兼容，加密输出完整 `mpw:` 密文，解密输出还原的明文。
- 本次仅提供工具类及其独立打包产物，不扩展配置解密来源或动态刷新能力。
- 保留仓库现有未提交修改。

## Open questions
无。用户已明确确认包含解密能力的更新需求。
