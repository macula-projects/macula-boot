# Plan: Alibaba Gateway 加解密前端示例 (from spec.md 2026-11-13)
Status: accepted

## Files that change
以下路径均相对 macula-boot-examples/macula-example-alibaba-gateway/：
- src/main/resources/static/crypto-demo/index.html：中文演示页面。
- src/main/resources/static/crypto-demo/demo.css：桌面和移动布局。
- src/main/resources/static/crypto-demo/crypto-client.js：协议适配及浏览器加解密。
- src/main/resources/static/crypto-demo/demo.js：表单、请求、状态与结果展示。
- src/main/resources/static/crypto-demo/request.js：基于 Axios 实例的统一请求入口，注册真正的请求/响应拦截器。
- src/main/resources/static/crypto-demo/vendor/axios.min.js、AXIOS_LICENSE：固定版本 Axios 浏览器发行文件与许可证。
- src/main/resources/static/crypto-demo/vendor/sm2.js、sm4.js、LICENCE_MIT、README.md：固定 sm-crypto 0.5.8 的浏览器发行文件、许可证、来源与校验值。
- src/main/resources/application.yml：local 加密保护配置及仅 /crypto-demo/** 静态资源认证放行。
- README.md：页面入口、依赖、运行步骤及保护接口调用说明。
- src/test/java/dev/macula/example/gateway/crypto/CryptoDemoIT.java：协议与真实过滤链集成验证。
- src/test/js/crypto-demo.test.cjs：Node 内置测试运行器的协议及请求构建验证，无 npm 工程。
- src/test/resources/crypto-demo/interop.json：仅含虚构数据的跨语言测试向量。
SDLC 文件：本目录 intent.md、spec.md、plan.md、review-findings.md。

## Order of work
1. 下载并核对 sm-crypto 0.5.8 官方浏览器发行文件与 MIT 许可证，记录来源及 SHA-256；固定同源静态资源，不依赖运行时 CDN，不新增 Maven/npm 依赖。
2. 实现协议适配：浏览器 crypto.getRandomValues 生成 16 字节 SM4 密钥；SM2 使用与 Hutool 一致的密文顺序及 04 点前缀；SM4 使用 ECB、PKCS7、UTF-8 与 Base64。通过双向互通测试确定具体格式，避免凭默认值推断。
3. 实现页面：加载内置公钥与清单；GET 输入 name/str，POST 编辑 UserResult JSON；Token 输入为 password，仅驻留内存；展示脱敏请求、响应及解密结果，用 textContent 安全输出。处理非法 JSON、初始化失败、HTTP/业务错误、解密错误和请求中状态。
4. GET 使用单路径段编码及 data Query 编码；拒绝空路径变量及会改变路径结构的输入。加密明文为原始 str=值，不预先编码值，避免过滤器整体编码造成二次编码。验证中文、空格、百分号；与号、加号和双引号在确认可无损往返前明确提示限制并拒绝发送。若实际互通结果要求调整允许字符，记录证据，不私自修改 Starter。
5. local 配置 crypto-switch=true、force-crypto=true、protect-urls.crypto 的两条原始完整路径：/consumer/api/v1/consumer/echo/*、/consumer/api/v1/consumer/user；protect-urls.sign 留空。保留现有 ignore-urls 并仅加入 /crypto-demo/**，user 的 OAuth 要求不变。
6. 补充集成测试、Node 测试及 README；在可用浏览器检查桌面/移动布局及实际按钮交互。具备外部依赖时用页面调用真实服务，否则报告未验证部分。

## Risks
- local 原明文调用将被拒绝，README 同步替换；移除新增配置和静态资源即可撤回本示例。
- SM2 密文顺序、前缀和 SM4 编码必须实测；公钥变化后可重新加载，失败不自动重放业务请求。
- 现有 CryptoGlobalFilter 对整个 Query 编码及响应发布类型有约束，发现框架缺陷则记录并征求范围调整。
- echo 仍匿名放行，user 仍需有效 Token；前端不存储 Token，不展示或打印明文 SM4 密钥。
- 真实调用依赖 Nacos、Redis、consumer、provider1 和认证环境；下游桩不能证明真实服务运行。

## Proof
- node --test macula-boot-examples/macula-example-alibaba-gateway/src/test/js/crypto-demo.test.cjs：加载实际 vendor 与 crypto-client.js，验证浏览器协议函数、特殊字符编码、请求结构、非法输入和错误响应处理。Node 不可用时明确报告未验证，不以跳过冒充通过。
- CryptoDemoIT：启动最小测试上下文，不加载业务主类及外部自动配置；调用 Node 加载实际 vendor 与 crypto-client.js，生成密文供 Hutool 解密，并将 Hutool 生成的密文传给同一 JS 适配函数解密；互通结果必须实际断言，不能仅验证静态 fixture。Node 不可用时测试明确失败并说明前置条件。真实 Gateway 过滤链配合 HTTP 下游桩验证公钥/清单、GET 路径变量与解密 Query、POST 解密 Body、响应解密和缺加密头拒绝。测试关闭外部 Nacos/OAuth/Redis 调用，明确桩边界。
- mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am verify -Dit.test=CryptoDemoIT -Dfailsafe.failIfNoSpecifiedTests=false；不扩大到无关模块外部集成测试。
- 浏览器验证 /crypto-demo/index.html、两种表单、加载与失败提示、移动溢出；有条件时分别报告真实 consumer/provider 联调结果。


## Implementation notes
- 计划已获用户明确 YES 批准，Status 更新为 accepted；未获 Git 提交授权，未执行提交。
- 集成验证采用 WebHttpHandlerBuilder 与真实 FilteringWebHandler/GlobalFilter/StripPrefix 直接组装最小 HTTP 链，避免加载业务自动配置；测试从实际 application.yml 的 local 段绑定 GatewayProperties。
- Query 中文、空格与百分号在实际 HTTP 链中往返成功；与号、加号和双引号按计划在页面拒绝，未修改 Starter。
- 浏览器访问工具返回 host unavailable，未完成桌面/移动视觉检查；使用实际 demo.js 的 DOM 测试桩补验按钮行为，不将其称为浏览器视觉验证。
- 未调用真实 consumer/provider 或 OAuth；所有 HTTP 集成验证使用下游桩。

## Build verification
- Node 内置测试：7 项通过，0 失败、0 跳过（含实际 demo.js DOM 桩交互）。
- 目标及依赖 Maven verify：116 项单元测试与 5 项 CryptoDemoIT 通过，0 失败、0 错误、0 跳过。实际配置使用 Maven 过滤后的 classpath application.yml，避免源码 @profile.active@ 占位符影响 YAML 解析。
- 变更 Java Checkstyle 与 git diff --check 通过。浏览器视觉与真实 consumer/provider 联调仍未执行。
- 实现已落地，等待工程师确认后进入正式 sdlc-test 阶段；未自行批准门禁。

## Frontend interceptor refinement
用户要求改用前端拦截器以减少业务干扰。本轮保留已接受的业务范围、接口、文件清单和协议，实现方式调整在原 crypto-client.js 内完成：
- createCryptoFetch 包装原生 fetch，在请求/响应拦截阶段完成保护清单匹配、密钥加载与缓存、加密和解密。业务调用传普通 URL 与 JSON Body。
- 未保护及跨源请求不注入密钥、Token 或加密包装；加密请求禁止跳转，避免携带密钥头跳往其他来源。
- 每个请求独立保存密钥，元数据并发加载合并；reload 只刷新配置，不重放业务请求。
- 业务字段校验和结果核对仍在页面，通用拦截器不认识 name、str、userName、age。调试输出由可选 onTrace 回调提供。
- 使用现有 Node/IT 文件覆盖透明请求、未保护请求、并发密钥隔离、失败、取消以及实际过滤链。
此调整不改变 accepted spec 的交付行为，不新增外部依赖、后端端点或修改 Starter。

### Interceptor verification
- 12 项 Node 测试通过，0 失败、0 跳过；覆盖普通业务调用、Ant 清单匹配、未保护及跨源旁路、Request Body 不被旁路消费、并发加载与独立密钥、取消、HTTP/业务错误、重载及 DOM 按钮行为。
- 最终目标及依赖 verify 通过：116 项相关单元测试、5 项 CryptoDemoIT，0 失败、0 错误、0 跳过。GET/POST 通过实际 createCryptoFetch 跨 HTTP 调用真实过滤链，业务侧不再构造密文或解密。
- 变更 Java Checkstyle 0 违规，git diff --check 通过；浏览器视觉与真实 consumer/provider 联调仍未执行。

## Axios request.js refinement
用户明确确认采用 Axios 封装 request.js。本轮将 fetch 包装实现替换为 Axios 实例及 request/response interceptors，保留同一业务接口与加解密协议。crypto-client.js 仅保留协议函数；request.js 管理 Axios、元数据、每次请求的密钥上下文。业务使用 request.get(url,{params})、request.post(url,data)，直接获得统一 Result。Axios 同源固定版本分发并保留 MIT 许可证，不新增 npm 工程。测试与 README 同步采用真实 Axios 实例。

### Axios verification
- 固定 Axios 1.21.1 官方浏览器发行文件与 MIT 许可证，按 npm SHA-512 integrity 和本地 SHA-256 核对；未修改第三方文件。
- 12 项 Node 测试使用真实 Axios 实例、请求/响应拦截器和默认数据转换，仅以 adapter 隔离网络；全部通过，0 失败、0 跳过。
- 最终 Maven verify 通过：116 项相关单元测试与 5 项 CryptoDemoIT，0 失败、0 错误、0 跳过。GET/POST 使用实际 Axios 官方 fetch adapter 跨 HTTP 调用网关真实过滤链。
- 变更 Java Checkstyle 0 违规，资源引用、第三方校验值及 git diff --check 通过。浏览器视觉检查和真实 consumer/provider/OAuth 联调仍未执行。

## Test stage
用户已明确 YES 确认 Axios 实现完成。本轮正式执行 sdlc-test，证据见 verification.md：全仓 334 项单元测试、12 项前端测试与 5 项目标 IT 通过。完整验证尚缺隔离环境下的全仓集成测试、移动/POST 浏览器检查及真实服务联调；未自行标记 green 或移交 Deploy。

## 前端样板整理

2026-11-13，用户确认示例已可运行，要求整理前端格式并说明复用边界。对已有 crypto-client.js、request.js、demo.js、index.html、demo.css 统一四空格格式，补充模块职责与接入 API 注释；模块 README 增加文件复用表、最小接入步骤及适用限制。不改业务接口、加密协议、认证边界或 vendor 原始发行文件。使用已有本地 Prettier 执行格式化，不引入 npm 工程或依赖；复跑现有 Node 测试并检查 vendor 无变更。

回归验证发现原始 HEAD 的 Node 测试已有 11 通过、1 失败：页面未填 Token 时直接发出 POST，与原测试和页面说明不符。本轮补回 demo.js 的 POST Token 非空提示，仅作用于演示表单，不替代服务端认证，也不改变通用 request.js 的认证规则。

整理后验证：五个自有前端文件 Prettier --check 通过；Node 原有 12 项测试全部通过（0 失败、0 跳过）；vendor 无 diff；git diff --check 通过。未重跑 Java 集成测试或浏览器视觉检查，本轮未修改密码算法或后端协议。

## 已接受的签名扩展

用户明确批准 [signing-plan.md](signing-plan.md)，并补充普通接口需求。该已接受计划的文件集合、顺序、风险和验证要求作为本计划的扩展，包含 consumer Controller、配置和 README。

签名实现补充：现有 observability 回声脚本原先使用明文 curl，强制验签后无法到达 provider。为保留该链路，脚本复用实际 request.js 发出签名加密请求并捕获原始响应头，明确增加 Node.js 前置条件，不降低网关保护。网关健康检查改用普通 plain 接口；协议过滤器日志级别设 INFO，避免 DEBUG 输出明文密钥。
