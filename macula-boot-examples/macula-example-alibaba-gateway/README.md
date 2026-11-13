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

local 环境可通过 `NACOS_SERVER_PORT` 覆盖 Nacos 端口，共享环境可通过 `NACOS_SERVER_ADDR` 覆盖完整地址；命名空间和认证使用 `NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD`。IDE 与 Docker 统一监听 HTTP `5099` 和 HTTPS `5443`；HTTP 端口可通过 `SERVER_HTTP_PORT` 覆盖。Compose 映射和健康检查使用相同的 `5099` 端口。

local/docker 默认使用本地示例凭据 `nacos/nacos`，与 examples Docker 的 `mysql-init` 用户初始化一致。`NACOS_USERNAME`、`NACOS_PASSWORD` 只覆盖客户端凭据，不会自动创建用户或修改已有密码。若使用已有数据卷，请先执行 Docker README 中的用户初始化步骤；修改后需重启应用。

## 路由

| 入口 | 目标 |
| --- | --- |
| `/consumer/**` | `lb://macula-example-alibaba-consumer` |
| `/websocket/**` | `lb:ws://macula-example-alibaba-consumer` |

local 配置中的 `echo/*` 和 `user` 已启用强制加密，原缺少加密请求头的明文 curl 会被拒绝。请使用下方页面演示。

网关安全配置中包含演示用 client secret，部署前必须通过配置中心替换。


## 浏览器加解密示例

启动依赖后打开 **http://127.0.0.1:5099/crypto-demo/index.html**，也可通过 HTTPS `https://localhost:5443/crypto-demo/index.html` 访问（本地示例证书需要浏览器信任）。页面、Axios 和加密库全部由 Gateway 同源提供，不需要 npm 安装、前端构建或运行时 CDN。

### 运行条件

- 启动 Nacos、Redis、`macula-example-alibaba-provider1`、`macula-example-alibaba-consumer` 和当前 Gateway，使用 local 配置。
- GET echo 和普通 plain 已在网关认证白名单中；POST user 保留现有认证规则，需要准备有效 OAuth Bearer Token 及可用的 introspection 服务。页面不会替代登录流程。
- 使用现代浏览器和 localhost/HTTPS，必须支持 `crypto.getRandomValues`。Token 只保留在页面内存，不写 localStorage/sessionStorage，不在请求预览中完整回显。
- 公钥由现有 `GET /gateway/protect/key` 提供，保护清单由 `GET /gateway/protect/urls` 提供。重启 Gateway 后点击“重新加载”取得新公钥。

### 保护配置

local 段使用下列配置；路径是 **StripPrefix 前的完整网关入口路径**：

```yaml
macula:
  gateway:
    crypto-switch: true
    force-crypto: true
    sign-switch: true
    force-sign: true
    protect-urls:
      crypto:
        - /consumer/api/v1/consumer/echo/*
        - /consumer/api/v1/consumer/user
      sign:
        - /consumer/api/v1/consumer/echo/*
        - /consumer/api/v1/consumer/user
```

`security.ignore-urls` 在原白名单上增加 `/crypto-demo/**` 与普通接口 `/consumer/api/v1/consumer/plain`，允许页面和静态资源加载；`/user` 不匿名放行。加密保护与 OAuth 认证分别执行。两条保护接口默认同时加密和签名，旧版仅加密请求会被强制验签拒绝。普通 plain 接口不在 crypto/sign 清单中。Nacos 配置可能覆盖本地配置，页面显示的保护清单应与上述配置一致。

### 作为业务项目样板复用

| 文件 | 是否直接复用 | 接入时的处理 |
| --- | --- | --- |
| `crypto-demo/request.js` | 同一网关协议下可直接复用 | 创建全局共享请求实例，将 `getToken` 接入已有登录态；业务调用普通 `get/post`。 |
| `crypto-demo/crypto-client.js` | 同一协议下可直接复用 | 保持 SM2/SM4 模式、编码和 Result 成功码与服务端一致，不在业务代码里手动加解密。 |
| `crypto-demo/vendor/axios.min.js`、`sm2.js`、`sm4.js` | 可随上述两个文件一起复用 | 保留对应 MIT 许可证和版本信息；已有依赖管理的工程可改用等价模块依赖。 |
| `crypto-demo/demo.js` | 参考调用方式 | 表单、DOM、超时展示、响应字段核对和明文面板均属于演示页面，按实际业务重写。 |
| `crypto-demo/index.html`、`demo.css` | 参考页面结构和样式 | 不是请求库的依赖，可替换为 Vue、React 或项目已有页面。 |

文件路径均相对 `src/main/resources/static/`。最小依赖为三个 vendor 脚本和两个自有请求脚本，无需复制演示页面。普通浏览器按以下顺序加载：

```html
<script defer src="/crypto-demo/vendor/sm2.js"></script>
<script defer src="/crypto-demo/vendor/sm4.js"></script>
<script defer src="/crypto-demo/vendor/axios.min.js"></script>
<script defer src="/crypto-demo/crypto-client.js"></script>
<script defer src="/crypto-demo/request.js"></script>
<!-- 在后续业务脚本中创建一次 request 实例，再执行接口调用。 -->
```

初始化和业务调用参考下一节。接入时注意：

1. 页面与 Gateway 同源，`baseURL` 使用页面 origin 或同源网关地址。当前实现不提供跨源加密；不要把修改 `baseURL` 当成跨域支持。
2. `getToken` 返回现有登录态中的 Token；本封装只在命中加密或签名保护清单的请求中自动附加 Token。其他需认证的普通接口，应由业务已有认证逻辑提供 `Authorization`。
3. 省略 `onTrace`。它用于演示明文和密文，可能接收业务敏感数据；生产业务不复制明文面板。
4. 后端需提供 `/gateway/protect/key`、`/gateway/protect/urls` 以及本文的加密响应协议，配置真实的保护路径。请求实例不会自动为新业务接口配置网关保护。
5. 默认保留官方 `fetch` adapter；它通过 `redirect: 'error'` 禁止加密请求重定向。仅支持文中列出的 JSON、Query 与统一 Result 场景，不作为上传、下载、流或任意 Axios 配置的通用封装。
6. 这两个自有文件当前同时支持浏览器全局变量和 CommonJS，未直接导出 ES Module。Vite/Vue/React 工程接入时需按项目模块规范调整 `import/export`，不要原样作为 ESM 导入。

生产接入仍需 HTTPS、身份认证，以及与真实后端的联调。现有 Node 测试和 `CryptoDemoIT` 验证本协议及拦截行为，不代表适用于其他网关协议。

### Axios 统一 request.js

`request.js` 基于真实 Axios 实例注册请求／响应拦截器，业务直接调用 `request.get/post`。`crypto-client.js` 提供加解密、SHA-256 签名及协议工具，不负责发请求，也不认识具体业务接口。

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
2. 按原始完整路径匹配 `protectUrls.crypto`。未匹配 crypto/sign、跨源及元数据请求保持普通 Axios 行为，不附加 Token 或加密材料；加载失败拒绝请求，不退回明文。
3. 请求拦截器在 Axios 默认序列化前处理普通 `params/data`，每次生成独立 SM4 密钥。密钥保存在以 Axios config 为键的 WeakMap 中，不挂在可序列化 config、请求参数或调试输出里。显式 Authorization 优先，否则读取 `getToken()`。
4. GET/DELETE 将普通 Query 转成 `data` 密文；POST/PUT/PATCH 将 JSON 包装为 `{data:密文}`；命中签名清单时先固定最终 JSON 文本并签名，再直接发送该文本，避免后续 transformRequest 改变 Body。使用 Axios 官方 fetch adapter，并明确禁止加密请求重定向；不会自动重试。
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

### 签名演示与普通请求对照

| 页面场景 | 接口 | 加密 | 签名 | OAuth |
| --- | --- | --- | --- | --- |
| 普通 GET | `/consumer/api/v1/consumer/plain?str=...` | 无 | 无 | local 示例匿名 |
| 保护 GET | `/consumer/api/v1/consumer/echo/{name}` | SM4 Query 与响应 | SHA-256 | local 示例匿名 |
| 保护 POST | `/consumer/api/v1/consumer/user` | SM4 JSON 与响应 | SHA-256 | 有效 Bearer Token |

普通接口可以单独用 curl 验证（直连 consumer 为 `http://localhost:5010/api/v1/consumer/plain`）：

```bash
curl --get 'http://localhost:5099/consumer/api/v1/consumer/plain' --data-urlencode 'str=A&B+中文'
```

统一 Result 的 `data` 应为 `Plain A&B+中文`。普通接口只依赖 Gateway 和 Consumer，不调用 provider；Query 使用标准 URL 编码，不受加密 Query 的 `&`、`+` 限制。它仅用于对照演示，不加入 crypto/sign 清单。页面先读取保护清单，若远程配置把 plain 覆盖到保护规则中，会提示配置错误。

业务仍使用同一个 request 实例，不需要手动生成签名：

```javascript
const plainResult = await request.get('/consumer/api/v1/consumer/plain', {
    params: { str: 'A&B+中文' }
});
console.log(plainResult.data); // Plain A&B+中文
```

保护请求会自动发送 `sm4-key`、`sym-alg`、`signature`、`timestamp`、`nonce`、`algorithm`。页面签名区显示 SHA-256、毫秒时间戳、安全随机 nonce、签名及 POST Body 摘要；不输出包含明文密钥的完整签名输入。

验签公式以当前 `SignCheckGlobalFilter` 为准，路径使用 StripPrefix 前的网关原始路径：

```text
Query = 按参数名排序，取各名称首值，忽略空首值，拼为 name=value&...
GET   = hex(SHA256(path + Query + key + timestamp + nonce))
POST  = hex(SHA256(path + Query + "SHA-256=" + hex(SHA256(finalBody)) + key + timestamp + nonce))
```

GET 签的是 `data=密文`，POST 签的是最终发送的 `{"data":"密文"}` 字节，不是业务明文。SHA-256 摘要使用 UTF-8 和十六进制；此签名不是 HMAC，也不需要浏览器持有固定应用 secret。每次请求使用独立 key 和 nonce，不自动重试。

验签使用 Redis 查重，时间窗口为 300 秒。测试覆盖缺头、密文/Body 篡改、过期和重复 nonce 拒绝。`CryptoDemoIT` 使用真实验签过滤器和内存 nonce 测试替身，不验证真实 Redis TTL、网络或并发原子性；当前 Starter 的查重与写入不是单个原子操作。服务端签名、解密及 Body 过滤器的 DEBUG 日志可能包含密钥或业务内容，正式接入时保持 INFO 及以上级别。

### 协议与验证

浏览器使用固定版本 `Axios 1.21.1`（MIT）和 `sm-crypto 0.5.8`，SM2 为 C1C3C2（补回 `04` 点前缀），SM4 为 ECB/PKCS7、UTF-8、Base64。第三方 MIT 许可证、来源和校验值见 [vendor/README.md](src/main/resources/static/crypto-demo/vendor/README.md)。该模式沿用现有后端协议，加密不替代 HTTPS 或身份认证。

在仓库根目录执行（需要 Node.js 18+ 和 Java 17）：

```bash
node --test macula-boot-examples/macula-example-alibaba-gateway/src/test/js/crypto-demo.test.cjs
mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am verify -Dit.test=CryptoDemoIT -Dfailsafe.failIfNoSpecifiedTests=false -Dgpg.skip=true
```

`CryptoDemoIT` 加载实际浏览器加密适配代码与 Hutool 双向交换密文，并使用实际 Gateway 过滤链和本机 HTTP 下游桩验证请求、响应及缺头拒绝。它使用内存 nonce 测试替身，不连接真实 Nacos、Redis、OAuth、consumer 或 provider，不能代替上述页面真实服务联调；Node 缺失时明确失败。GET/POST 集成测试由实际 Axios request.get/post 发出普通业务请求，自动完成双向加解密；Node 测试使用真实 Axios 实例和拦截器，仅将网络 adapter 替换为测试桩。

## 版权说明

本模块自有代码遵循项目 Apache License 2.0；`sm-crypto` 和 Axios 浏览器资源各自保留其 MIT 许可证。
