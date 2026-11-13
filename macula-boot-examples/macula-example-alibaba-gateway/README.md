# Alibaba Gateway 示例

该模块演示 `macula-boot-starter-cloud-alibaba-scg` 的网关接入：从 Nacos 发现 consumer，转发 HTTP 和 WebSocket 请求，并接入 Sentinel 与 OAuth2 资源服务器。

模块使用统一 Observability Starter；OTLP 网络导出默认关闭，配置 Docker observability overlay 时开启。WebFlux 已启用 Reactor 自动上下文传播。

## 前置条件

- Nacos，默认地址 `127.0.0.1:38848`、命名空间 `MACULA5`。
- 已启动 `macula-example-alibaba-consumer`。
- HTTPS 使用 `src/main/resources/jwk/springboot.p12` 中的示例证书，仅供本地演示。

## 启动

```bash
# 在仓库根目录先安装所需框架依赖，再只启动当前应用
mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am install -DskipTests -Dgpg.skip=true -Pdeploy
mvn -f macula-boot-examples/macula-example-alibaba-gateway/pom.xml spring-boot:run
```

local 环境可通过 `NACOS_SERVER_PORT` 覆盖 Nacos 端口，共享环境可通过 `NACOS_SERVER_ADDR` 覆盖完整地址；命名空间和认证使用 `NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD`。应用同时监听 HTTP `5000` 和 HTTPS `5443`。

## 路由

| 入口 | 目标 |
| --- | --- |
| `/consumer/**` | `lb://macula-example-alibaba-consumer` |
| `/websocket/**` | `lb:ws://macula-example-alibaba-consumer` |

local 配置中的 `echo/*` 和 `user` 已启用强制加密，原缺少加密请求头的明文 curl 会被拒绝。请使用下方页面演示。

网关安全配置中包含演示用 client secret，部署前必须通过配置中心替换。


## 浏览器加解密示例

启动依赖后打开 **http://127.0.0.1:5000/crypto-demo/index.html**，也可通过 HTTPS `https://localhost:5443/crypto-demo/index.html` 访问（本地示例证书需要浏览器信任）。页面、Axios 和加密库全部由 Gateway 同源提供，不需要 npm 安装、前端构建或运行时 CDN。

### 运行条件

- 启动 Nacos、Redis、`macula-example-alibaba-provider1`、`macula-example-alibaba-consumer` 和当前 Gateway，使用 local 配置。
- GET echo 已在网关认证白名单中；POST user 保留现有认证规则，需要准备有效 OAuth Bearer Token 及可用的 introspection 服务。页面不会替代登录流程。
- 使用现代浏览器和 localhost/HTTPS，必须支持 `crypto.getRandomValues`。Token 只保留在页面内存，不写 localStorage/sessionStorage，不在请求预览中完整回显。
- 公钥由现有 `GET /gateway/protect/key` 提供，保护清单由 `GET /gateway/protect/urls` 提供。重启 Gateway 后点击“重新加载”取得新公钥。

### 保护配置

local 段使用下列配置；路径是 **StripPrefix 前的完整网关入口路径**：

```yaml
macula:
  gateway:
    crypto-switch: true
    force-crypto: true
    protect-urls:
      crypto:
        - /consumer/api/v1/consumer/echo/*
        - /consumer/api/v1/consumer/user
      sign: []
```

`security.ignore-urls` 仅在原白名单上增加 `/crypto-demo/**`，允许页面和静态资源加载；`/user` 不匿名放行。加密保护与 OAuth 认证分别执行。本例仅演示加解密，不发送签名相关请求头。Nacos 配置可能覆盖本地配置，页面显示的保护清单应与上述配置一致。

### Axios 统一 request.js

`request.js` 基于真实 Axios 实例注册请求／响应拦截器，业务直接调用 `request.get/post`。`crypto-client.js` 只提供密码算法及协议工具，不负责发请求，也不认识具体业务接口。

```javascript
// 应用启动时创建一次；使用现有认证状态，不由加密模块持久化 Token。
const request = MaculaRequest.createRequest({
    getToken: () => currentAccessToken
});

const echoResult = await request.get('/consumer/api/v1/consumer/echo/demo', {
    params: {str: '你好 Macula'}
});
console.log(echoResult.data); // 已解密的字符串

const userResult = await request.post('/consumer/api/v1/consumer/user', {
    userName: 'demo-user',
    age: 25
});
console.log(userResult.data.userName); // 已还原的对象
```

此封装通过响应拦截器直接返回后端统一 `Result`，因此调用方读取 `result.data`，无需 `response.json()`，也无需再取一层 Axios `response.data`。HTTP 错误、超时、取消保留 `AxiosError`；业务失败（包括 HTTP 200）统一 reject，可由现有业务 `try/catch` 处理。

拦截流程：

1. 首次同源业务请求合并加载公钥和保护清单；独立的 Axios 元数据实例避免递归拦截。可调用 `await request.loadCryptoMetadata(true)` 刷新配置。
2. 按原始完整路径匹配 `protectUrls.crypto`。未匹配、跨源及元数据请求保持普通 Axios 行为，不附加 Token 或加密材料；加载失败拒绝请求，不退回明文。
3. 请求拦截器在 Axios 默认序列化前处理普通 `params/data`，每次生成独立 SM4 密钥。密钥保存在以 Axios config 为键的 WeakMap 中，不挂在可序列化 config、请求参数或调试输出里。显式 Authorization 优先，否则读取 `getToken()`。
4. GET/DELETE 将普通 Query 转成 `data` 密文；POST/PUT/PATCH 将 JSON 包装为 `{data:密文}`，交给 Axios 默认 transformRequest 序列化。使用 Axios 官方 fetch adapter，并明确禁止加密请求重定向；不会自动重试。
5. 响应拦截器解密成功响应的 `data`，对象／数组 JSON 还原为对象／数组，其余保留字符串。现有协议没有原始类型标记，恰好为对象／数组 JSON 的业务字符串也会被解析。
6. 请求成功或失败后释放该请求的密钥上下文，并发请求互不串线。可用 Axios 的 `signal` 取消业务请求；共享元数据加载不随单个请求取消。

保护接口仅支持 JSON 响应；FormData、文件上传／下载、流、GET Body 和混合 POST Query 明确拒绝。清单支持 Ant 的 `*`、`**`、`?` 和简单 `{变量}`，不支持带正则的 `{变量:正则}`。业务不要预包装 `data` 或主动添加加密头；Query 凭据请改为 Authorization。

演示面板通过可选 `onTrace` 回调展示过程，业务默认无需配置。回调不暴露 SM4 明文密钥，Authorization 脱敏；诊断回调失败不影响业务。生产业务不要开启明文面板。业务字段校验和结果核对仍在 `demo.js`。

### GET：明文路径变量 + 加密 Query

1. 页面自动加载公钥和保护清单，显示“已就绪”。
2. 输入 `name=demo`、`str=你好 Macula`，点击“加密并发送 GET”。
3. 浏览器生成单次请求 SM4 密钥，将 `str=你好 Macula` 加密成 Base64，使用 Query 参数 `data` 发送；`demo` 始终留在路径中。
4. 请求路径为 `/consumer/api/v1/consumer/echo/demo?data=密文`；`sm4-key` 为 SM2 加密后的十六进制密钥，`sym-alg=SM4`。
5. Gateway 解密并去除 `/consumer` 路由前缀，consumer 实际收到 `/api/v1/consumer/echo/demo?str=你好 Macula`。
6. 响应 `data` 是密文，页面还原后应包含 `Hello 你好 Macula`，并以 `,demo` 结尾。中间配置值、用户和时间由 provider1 决定。

当前 `CryptoGlobalFilter` 会移除解密明文中的双引号，并整体编码 Query，未对每个值单独编码。因此本演示对 `str` 中的 `&`、`+`、双引号和控制字符明确拒绝；中文、空格和 `%` 有互通测试覆盖。不要预先对 `str` 进行 URL 编码再加密。路径变量必须是单个非空路径段。

### POST：加密 JSON Body

1. 填写有效 OAuth Token（可以带或不带 `Bearer ` 前缀）。
2. 编辑虚构数据，例如 `{"userName":"demo-user","age":25}`，点击“加密并发送 POST”。
3. 浏览器加密完整 JSON，向 `/consumer/api/v1/consumer/user` 发送 `Content-Type: application/json` 和 `{"data":"Base64密文"}`。
4. Gateway 解密 Body 后传给 consumer，consumer 调用 provider1。页面应还原用户对象，`userName` 和 `age` 与输入一致；`birthday`、`password` 会被 provider1 更新。

每次请求展示明文、加密后的请求、响应原文和解密结果；失败时显示 HTTP/业务/解密错误，不自动重试。请勿使用真实密码或个人数据，本页的明文和结果面板用于开发调试。

### 协议与验证

浏览器使用固定版本 `Axios 1.21.1`（MIT）和 `sm-crypto 0.5.8`，SM2 为 C1C3C2（补回 `04` 点前缀），SM4 为 ECB/PKCS7、UTF-8、Base64。第三方 MIT 许可证、来源和校验值见 [vendor/README.md](src/main/resources/static/crypto-demo/vendor/README.md)。该模式沿用现有后端协议，加密不替代 HTTPS 或身份认证。

在仓库根目录执行（需要 Node.js 18+ 和 Java 17）：

```bash
node --test macula-boot-examples/macula-example-alibaba-gateway/src/test/js/crypto-demo.test.cjs
mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am verify -Dit.test=CryptoDemoIT -Dfailsafe.failIfNoSpecifiedTests=false -Dgpg.skip=true
```

`CryptoDemoIT` 加载实际浏览器加密适配代码与 Hutool 双向交换密文，并使用实际 Gateway 过滤链和本机 HTTP 下游桩验证请求、响应及缺头拒绝。它不连接真实 Nacos、Redis、OAuth、consumer 或 provider，不能代替上述页面真实服务联调；Node 缺失时明确失败。GET/POST 集成测试由实际 Axios request.get/post 发出普通业务请求，自动完成双向加解密；Node 测试使用真实 Axios 实例和拦截器，仅将网络 adapter 替换为测试桩。

## 版权说明

本模块自有代码遵循项目 Apache License 2.0；`sm-crypto` 和 Axios 浏览器资源各自保留其 MIT 许可证。
