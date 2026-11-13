# Spec: Alibaba Gateway 加解密前端示例 (from intent.md 2026-11-13)
Status: accepted

## Source intent
[已接受的 intent.md](intent.md)，结合用户补充：主要交付前端，复用 consumer 的 /echo/{name}、/user。

## Requirements
1. 在 Gateway 的 static 资源中提供中文 HTML/JS/CSS 页面，通过同源地址访问；不增加前端构建工程或 Java 客户端。
2. 页面读取内置 /gateway/protect/key 和 /gateway/protect/urls，展示公钥、保护清单及加载错误。
3. GET 表单输入 name 和 str：调用 /consumer/api/v1/consumer/echo/{name}，路径变量保留明文，仅将 str=... 加密到 data Query。
4. POST 表单编辑 UserResult JSON：调用 /consumer/api/v1/consumer/user，将 JSON 加密为 {"data":"密文"}。
5. 浏览器生成临时 SM4 密钥，以 SM2 加密密钥，发送 sm4-key、sym-alg；展示请求明文、请求密文、响应原文和响应 data 解密结果。支持输入 OAuth Bearer Token，仅保存在页面内存，不回显完整 Token 或明文密钥。
6. application.yml local 段配置 crypto-switch=true、force-crypto=true，protect-urls.crypto 包含 /consumer/api/v1/consumer/echo/* 和 /consumer/api/v1/consumer/user；protect-urls.sign 留空。
7. README 提供页面地址、启动依赖、操作步骤和预期结果，更新原明文 echo 调用说明。

## Non-goals
不新增业务端点、公钥接口、路由或 Profile；不修改 consumer/provider/Starter 协议；不扩展签名演示。

## Design
按用户确认使用 Axios 封装 request.js，请求和响应通过 Axios interceptors 统一加解密，业务使用 get/post 和普通 params/data。静态页面由 Gateway 提供，复用 /consumer/** 与 StripPrefix=1。仅为演示页面及其静态资源配置必要的匿名访问；现有 API 认证规则保持。浏览器加解密实现需与现有 Hutool SM2/SM4 模式和编码互通，实施计划明确库来源、版本和许可证。GET 正确编码路径段与 Query 密文，POST 使用 application/json。
适用规则：AGENTS.md、architecture.md、testing.md、REVIEW.md；未发现额外适用组织策略。

## Data and interfaces
现有接口与 UserResult 模型不变，响应外层 Result 保持、data 加密。GET 解密结果核对 str 和 name；POST 核对 userName、age，允许 provider1 更新 birthday、password。页面仅使用虚构示例数据。

## Flagged concerns
- local 行为变化：启用强制加密后，两个接口缺少加密头的调用被拒绝，non-blocking。
- 认证及依赖：echo 已匿名放行，user 仍需有效 Token；真实联调需要 Nacos、Redis、consumer、provider1 及认证环境，non-blocking。
- 现有协议约束：force-crypto 检查加密头；公钥随重启改变，页面可重新加载；阻断示例的框架缺陷另行报告范围，non-blocking。
- 浏览器库：需要验证与 Hutool 的模式、填充、密文顺序和编码互通；作者署名待确认，non-blocking。

## Verification strategy
验证浏览器与 Hutool 的 SM2/SM4 互通，检查页面桌面/移动布局、加载失败及错误显示。使用真实网关过滤链和明确标注的 HTTP 下游桩验证路径、参数解密、响应解密及缺头拒绝。具备完整环境时再操作页面调用真实 consumer/provider，分别报告静态检查、模拟验证和真实联调结果。
