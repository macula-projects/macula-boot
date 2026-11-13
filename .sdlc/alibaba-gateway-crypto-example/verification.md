# 验证报告：Alibaba Gateway Axios 加解密前端

日期：2026-11-13。用户已明确确认实现完成，本轮按 sdlc-test 执行。

## 结论
已执行的自动化检查全部通过；完整验证证据仍未齐全，不声明 Verification is green，不进入 sdlc-deploy。

## 实际命令与结果

### 前端

```bash
node --test macula-boot-examples/macula-example-alibaba-gateway/src/test/js/crypto-demo.test.cjs
```

退出码 0，实际输出：

```text
ℹ tests 12
ℹ pass 12
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
```

实际 Axios 实例、request/response interceptors、默认 transformRequest/transformResponse 均参与测试；仅网络 adapter 为桩。覆盖保护清单、普通 params/data、业务无关路径复用、并发元数据加载、密钥隔离、未保护/跨源旁路、错误、取消与实际 demo.js DOM 交互。

### 全仓单元测试

```bash
mvn test -Dgpg.skip=true
```

退出码 0，实际输出：

```text
[INFO] BUILD SUCCESS
[INFO] Total time:  02:13 min
```

按本次日志中的 82 条测试类结果汇总：334 项测试，Failures=0、Errors=0、Skipped=0。日志：/private/tmp/macula-crypto-test-all-unit.log。

### 目标集成验证

```bash
mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am verify -Dit.test=CryptoDemoIT -Dfailsafe.failIfNoSpecifiedTests=false -Dgpg.skip=true
```

退出码 0，CryptoDemoIT 实际输出：

```text
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

目标及依赖另执行 116 项单元测试（与全仓单元测试重复，不重复计入）。5 项 IT 使用实际 JS/Axios 官方 fetch adapter、Hutool、FilteringWebHandler、BodyGlobalFilter、CryptoGlobalFilter、StripPrefix 与临时 HTTP 下游桩，覆盖内置端点、保护清单、明文路径变量、Query/Body 解密、响应解密及缺头拒绝。测试从 Maven 过滤后的真实 application.yml local 段绑定配置。

### 静态检查

```bash
mvn -N checkstyle:check -Dcheckstyle.includes=macula-boot-examples/macula-example-alibaba-gateway/src/test/java/dev/macula/example/gateway/crypto/CryptoDemoIT.java
node --check macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/static/crypto-demo/request.js
node --check macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/static/crypto-demo/demo.js
git diff --check
```

退出码均为 0。Checkstyle 实际输出：

```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

## 浏览器证据与边界

浏览器连接器 host unavailable，但本轮使用原生 Chrome 打开隔离 UI 桩 http://127.0.0.1:18764/crypto-demo/index.html，观察到：

- 页面、公钥和保护清单加载成功，桌面布局正常。
- GET 点击/键盘执行后显示「往返成功 · 拦截器已自动加解密，业务结果已核对」。
- 请求路径仍为 /consumer/api/v1/consumer/echo/demo，Query 变为 data 密文。
- 响应 data 为密文，页面解密结果为「Hello 你好 Macula,demo」。

此浏览器验证使用 Node HTTP 桩，不是真实 consumer/provider，也不是真实 Gateway。真实 Gateway 过滤链证据来自上述 IT。

用户在验证过程中操作并关闭测试标签页，原生控制检测到用户切换到其他页面；因此停止 UI 操作，未抢占用户浏览器。浏览器 POST、错误提示和移动布局未完成；DOM 桩覆盖按钮和错误行为，但不能替代浏览器视觉验证。临时 UI 桩已停止。

## Proof / spec 对照

| 验证项 | 证据 | 状态 |
| --- | --- | --- |
| 实际 Axios params/data 与透明拦截 | Node、IT | 通过 |
| 内置公钥及保护清单 | IT、浏览器 UI 桩 | 通过 |
| 路径变量保持明文、中文/空格/百分号 Query | IT、浏览器 GET | 通过 |
| JSON Body 加密、响应对象解密 | Node、IT | 通过；真实业务服务未联调 |
| 缺头拒绝、HTTP/业务失败、取消和并发 | Node、IT | 通过 |
| 页面状态与按钮 | DOM 桩、浏览器加载/GET | 部分完成 |
| 桌面/移动视觉检查 | 桌面截图观察 | 移动未完成 |
| 全仓单元测试 | mvn test | 通过 |
| 全仓集成测试 | 需隔离 Redis 环境 | 未执行 |

## 未满足的环境及后续验证

- 未执行全仓 mvn verify：Redis、cache、idempotent、leader-election 的 IT 默认连接 localhost:6379，其中有通用 key/value 写入及缓存 clear。本机 6379 已有用户 Redis 实例，未确认是隔离测试库；不对其运行可能改动现有数据的测试。需要隔离的 CI/容器测试环境再完成全仓集成验证。
- 真实 consumer/provider/OAuth 链路未验证。本轮没有启动或部署业务应用，也没有使用真实 Token。
- 移动布局和浏览器 POST 需在可独立使用的浏览器环境补齐。

## 配置及 eval

本变更修改示例 application.yml 的 local 加密保护配置与页面资源白名单，新增固定版本 Axios/sm-crypto 静态依赖。未修改 CLAUDE.md、技能、hook、REVIEW.md 或 reviewer 定义，无适用的受保护配置 eval；未发现相关项目 eval suite，未编造执行结果。

macula-boot-examples/docker/.env.example 是本轮开始时已有的用户改动，未修改或回滚，不计入本功能验证范围。未提交、推送或发布。
