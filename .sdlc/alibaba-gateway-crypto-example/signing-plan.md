# Plan: 加解密样板补充签名演示
Status: accepted

## Source and scope

用户于 2026-11-13 明确要求“把签名演示也一起补上”。这是原 spec.md 中“不扩展签名演示”的范围修订提案；原已接受计划及现有实现继续保留，本文件等待用户确认后再纳入实施。

目标是让现有 GET echo、POST user 演示加密与签名同时生效，并让同一套 request.js 可被业务项目参考。保持业务 get/post 调用方式，不引入新业务端点，不改 Starter 验签协议，不演示 Kong AK/SK HMAC 认证。

## Protocol evidence

以当前源码 SignCheckGlobalFilter 为准：

- 使用 StripPrefix 前的原始网关路径，如 `/consumer/api/v1/consumer/echo/demo`。
- 签名头为 `signature`、`timestamp`、`nonce`、`algorithm`，沿用 `sm4-key` 传递 SM2 加密后的单次 SM4 密钥。
- timestamp 为毫秒时间戳；nonce 使用安全随机源。服务器允许的时间窗口为 300 秒，nonce 通过 Redis 记录。
- 外层算法选 SHA-256，输出十六进制。此处不是 HMAC，也不在浏览器放置固定的应用 secret。
- Query 使用实际传输参数：参数名按 Java TreeMap 顺序排序，每个名称仅取首值，空值不参与，拼接 `name=value` 后以 `&` 连接。
- GET 签名输入：`原始路径 + 排序后的密文Query + 单次SM4密钥 + timestamp + nonce`。
- POST 签名输入：`原始路径 + 排序后的Query + "SHA-256=" + 实际Body字节的SHA-256十六进制摘要 + 单次SM4密钥 + timestamp + nonce`。
- POST 必须对最终发送的 JSON 密文包装计算摘要，不能对明文或二次序列化后不同的 JSON 签名。
- 页面可展示算法、时间戳、nonce、签名、Body 摘要和验签结果，不展示包含明文密钥的完整签名输入。

## Files that change

路径相对 macula-boot-examples/macula-example-alibaba-gateway/：

- `src/main/resources/static/crypto-demo/crypto-client.js`：SHA-256、安全 nonce、规范化签名辅助函数；使用现有浏览器 Web Crypto，不增加第三方依赖。
- `src/main/resources/static/crypto-demo/request.js`：匹配 `protectUrls.sign`，在加密及最终序列化后生成签名，确保摘要与实际传输一致；保留 get/post、Token、取消和错误语义。
- `src/main/resources/static/crypto-demo/demo.js`：展示签名字段和网关返回结果，保留 GET/POST 表单与仅内存 Token。
- `src/main/resources/static/crypto-demo/index.html`、`demo.css`：补充签名说明和展示区，沿用现有格式与布局。
- `src/main/resources/application.yml`：local 显式开启 sign-switch、force-sign，将现有两条加密路径同时加入 sign 保护清单；docker 沿用 local。
- `README.md`：补充签名协议、调用步骤、可复用文件、Body 序列化及 Redis 边界，替换原“不发送签名”说明。
- `src/test/js/crypto-demo.test.cjs`：正确签名、Body 摘要、路径与 Query 规范化、并发 nonce、错误和取消回归。
- `src/test/java/dev/macula/example/gateway/crypto/CryptoDemoIT.java`：实际 SignCheckGlobalFilter 与 CryptoGlobalFilter 链路联调，验证成功、缺签名、篡改、过期、重复 nonce。
- 必要时更新 `src/test/resources/crypto-demo/interop.json`，使用虚构数据作为签名互通向量。
- SDLC：确认后同步本目录 intent.md、spec.md、plan.md，记录范围变更及实际验证证据。

## Order of work

1. 明确修订原 non-goal，确认本计划后更新正式范围文件。
2. 实现签名辅助函数和请求拦截器；GET 签名实际密文 Query，POST 固定最终发送文本后签名。
3. 更新保护配置、页面签名展示及样板接入说明。
4. 执行现有及新增 Node 测试，再执行 CryptoDemoIT；格式检查并刷新应用运行资源。

## Risks

- 配置强制验签后，这两条示例路径的旧版“只加密”请求将被拒绝；新样板默认自动加密并签名。
- 后端验签使用原始路径及密文，正文摘要使用十六进制；旧 Javadoc 中的 Base64 描述不能作为实施依据。
- Web Crypto 需要 localhost 或 HTTPS；不支持时明确报错，不退回弱算法。
- 验签依赖 Redis。集成测试可用独立的 nonce 存储测试替身检验分支，但必须明确它不能证明真实 Redis 行为和并发防重放原子性。
- 当前 Starter 使用先检查 nonce 再写入的方式；本轮不修改该协议或宣称提供原子防重放保证。
- 签名材料包含单次密钥，诊断只展示脱敏输入，不能输出完整拼接串或持久化密钥。

## Proof

- Node 测试：保留现有 12 项行为，新增签名规范化、最终 Body 字节摘要、并发请求隔离和 SHA-256 互通断言。
- CryptoDemoIT：使用真实 Gateway 过滤链，验证 GET/POST 加密签名成功及缺签名、篡改、过期、重放拒绝；明确下游与 Redis 替身边界。
- `node --test macula-boot-examples/macula-example-alibaba-gateway/src/test/js/crypto-demo.test.cjs`。
- `mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am verify -Dit.test=CryptoDemoIT -Dfailsafe.failIfNoSpecifiedTests=false -Dgpg.skip=true`。
- 具备服务条件时页面真实联调，分别报告真实 Redis/OAuth/consumer/provider 的执行结果，不以桩测试替代声明。

## 用户确认与普通接口补充

用户明确回复 YES，并要求补充不加密、不签名的普通接口。新增 consumer 的 GET /api/v1/consumer/plain（经网关为 /consumer/api/v1/consumer/plain），只返回输入回声，不调用 provider。将该路径加入 consumer 与网关 local 匿名清单，不加入 crypto/sign 清单。页面通过同一 request 实例提供普通请求表单，测试确认参数、响应和无签名/加密头。追加 consumer 的 ConsumerController.java、application.yml、README.md 到文件清单；其余来源和边界保持。

## 实施与验证结果

- 自动签名和普通 plain 接口、页面对照已实现，保留业务 get/post 调用。
- Node 14 项测试通过，0 失败、0 跳过。
- CryptoDemoIT 7 项通过，0 失败、0 错误、0 跳过；覆盖真实验签/解密过滤器和普通 HTTP 路径。Redis 为内存 nonce 测试替身；未进行真实 OAuth、Redis、Nacos、consumer/provider 的页面联调。
- 首次集成验证被沙箱禁止绑定临时回环端口；获得扩展权限后重跑成功。期间修正了原测试将 YAML 列表按标量读取的问题。
- gateway 与 consumer 的目标及依赖 Maven verify 成功（最终定向 Surefire 仅运行 OptionalTest；更早执行的依赖单元测试也通过），前端格式检查及变更 Java Checkstyle 通过。
- 用户补充端口要求后，统一 HTTP 5099，保持 HTTPS 5443；Docker 健康检查通过 plain 路径，observability 脚本复用真实请求库发送加密签名回声。脚本仅通过语法检查，未连接 Collector/Loki/Tempo 做真实验收。
