# TinyID 网关接入评审

## 最终复审：2026-10-09

依据最新已接受范围及 verification.md 最新复验，sdlc-reviewer 对两仓实际变更执行 REVIEW.md 的 Bugs、Security、Compliance 三项检查，未发现新增 Important 或 Minor 阻断。

- Bugs：Feign 条件装配、号段协议、异常结果码、Controller/Advice、前端 Result 消费符合最终范围，发号算法保持。
- Security：四个发号入口均受认证保护，精确路由及共享 HMAC/JWT 保留；HMAC/个人 Token 边界仍是用户明确延期事项，不标记为已修复。
- Compliance：ROOT 管理权限及审计未改，未发现适用控制违规，无额外已声明监管要求。
- Nit（2 项，均已修正）：两仓 spec/plan 顶部过期“未测试”状态改为最新报告引用；Gateway README 清除“其他旧发号接口不增加路由”的历史措辞。
- 用户明确授权“完成提交合并与分支清理”；此次不包含生产部署。

以下保留早期评审历史；当前结论以上节为准。

最新处置：用户明确要求撤销专属 HMAC/个人 Token 区分，留待后续统一处理。已删除本次过滤器及其测试；原 Important 的代码链证据保留为历史，此项是用户决定延期处理，不标记为已修复。共享验签和 JWT 校验保持。

状态：用户已批准修复，代码及回归用例已补充，尚待正式验证与复审；下文保留原评审证据，不视为已关闭。未提交、推送或创建远程 PR。

## 范围与依据

- 依据：[spec.md](spec.md)、[plan.md](plan.md)、[verification.md](verification.md)。
- 检查 Boot 与 Cloud 独立 worktree 中的实际差异和新增未跟踪文件；两仓均使用 feat/tinyid-feign-gateway 分支。
- 按两仓 REVIEW.md 的 Bugs、Security、Compliance 三项，由独立 sdlc-reviewer 执行；最多 5 条 Nit。
- 已有验证通过：Boot 264 项（2 项无关外部服务跳过），Cloud 48 项，以及隔离环境真实发号链路。评审未重新运行构建；以下授权缺口来自代码链证据，未做新的动态复现。

## Important：普通用户身份可绕过应用签名授权

变更定位：Cloud `macula-cloud-tinyid/src/main/java/dev/macula/cloud/tinyid/controller/IdContronller.java:171`；`macula-cloud-gateway/src/main/resources/application.yml:45`。

新接口移除 token 校验后，仅依赖共享认证链，而共享链允许普通用户 Bearer：

1. Boot 网关 `ResourceServerAuthorizationManager` 对 Bearer 执行角色 URL 检查，默认 defaultUrlRequireCheck=false，无匹配规则即放行。
2. `KongApiGlobalFilter` 仅对 HMAC 请求执行应用签名与 URL 许可检查，普通 Bearer 跳过该分支。
3. `AddJwtGlobalFilter` 将正常认证的用户身份转换为网关签发的 JWT；Server 的 `ResourceServerConfiguration` 仅要求 authenticated()。
4. 新 Controller 没有额外身份或权限限制，因此普通用户可以申请已存在 bizType 的号段。

前提是用户具有可被网关正常 introspect 的有效 Token，且新路径未配置额外角色 URL 规则。用户无需持有应用 AK/SK 或应用 URL 许可；这超出了已接受的“获得接口许可的应用可申请任意 bizType”的权限范围。

建议人工确认新路由是否仅接受 HMAC 应用调用，再实施对应限制；若同时允许用户或 API Key 调用，需明确其独立授权要求。补充普通有效用户及无应用许可身份的拒绝测试，重新验证后再审。不得将本次已有绿灯测试当作这一边界的证明。

## Nit：拒绝测试缺少 HTTP 状态断言

定位：Cloud `macula-cloud-tinyid/src/test/java/dev/macula/cloud/tinyid/security/TinyIdSegmentSecurityIT.java:76`。

匿名、错误签名及过期 JWT 用例只断言号段服务未被调用，没有检查响应码，错误的 500 也可能通过。建议补充 401 断言。真实联调中的匿名 401 证据不能代替这三项持续回归断言。

## 三项结论与人工门禁

- Bugs：未发现其他 Important；默认拦截器组合、自定义号段服务退让、可选 MyBatis-Plus 实现及本地算法保持符合契约。
- Security：上述 Important 尚未解决。
- Compliance：未发现额外适用的数据分类、保留或监管要求；访问控制问题归入 Security，不重复列项。

下一步：人工确认接口身份边界后修复、补测并重新评审。不自动接受风险，不合并、不部署。
