# Plan: TinyID 评审修复与 ResultCode 异常补充（from spec.md）

最新用户撤销：下文 TinyIdHmacGlobalFilter 及 HMAC-only 拒绝用例不再实施，已移除；共享网关身份边界后续由用户统一处理。BizException 及其余已批准改动保留。

Status: accepted。用户已明确回复 yes；作为原计划的已接受补充。

用户已要求修复评审问题，并明确纠正异常基类为 BizException：TinyIdSysException 继承 BizException，复用其 ResultCode 构造契约与 code/msg。本文件确认具体实现边界，不自行批准新增公共 API 契约。

## Files that change

Boot：
- TinyID 模块 `base/exception/TinyIdSysException.java`、新增对应 `TinyIdSysExceptionTest.java`、README.md。
- `.sdlc/tinyid-feign-gateway/` 中 spec.md、plan.md、review.md、verification.md：获批后同步补充范围和实际证据。

Cloud 独立 worktree：
- Gateway 新增 `filter/TinyIdHmacGlobalFilter.java`，修改 `TinyIdGatewayIT.java`、README.md。
- TinyID `TinyIdSegmentSecurityIT.java`、README.md。
- 同步该任务的 SDLC 记录。不修改原 Cloud 工作区的 IAM 改动。

## Order of work

1. 在 TinyIdGatewayIT 中增加有效普通用户 Bearer、API Key/无 HMAC 头的拒绝场景，保留合法 HMAC、错误/过期签名及无 URL 许可的覆盖。
2. 新增 TinyID 专用网关过滤器：仅保护号段接口，在路径改写后、KongApiGlobalFilter 验签前执行；非 HMAC 请求返回 403。HMAC 头只是进入验签的条件，签名及 URL 许可仍必须由现有 KongApiGlobalFilter 校验，不重复实现密码学逻辑。不限制管理路由或其他服务。
3. 给 Server 安全测试的匿名、伪造及过期 JWT 场景补充 401 断言。
4. TinyIdSysException 直接继承 BizException，复用其 getCode()/getMsg()，不重复实现 ResultCode 或定义 code/msg 字段；保留现有四个构造方法，默认使用 ApiResultCode.SYS_ERROR，保留异常详情和 cause。新增接受 ResultCode 的构造方式（包括详情和 cause）；通过父类构造方法设置结果码，通过 initCause 保留原因，不改变 BizException 或 MaculaException。
5. 补充异常构造、默认/自定义结果码、消息与 cause 保留的单元测试；更新 README，说明未被业务代码捕获的异常可由已有 BizException Web 处理器识别，但不改变 TinyID Server 现有文本协议及捕获行为。
6. 获得实施完成确认后执行正式验证并重新评审；不提交、推送或部署。

## Risks

- 本次权限修复针对公开网关入口。Server 仍按现有契约信任有效 JWT；不能宣称新增了 Server 对 JWT 的 HMAC 来源识别。若需要 Server 独立拒绝普通用户 JWT，必须另行设计受签名保护的身份声明及其缓存隔离，不能依赖可伪造请求头。
- 不改公共 JWT 签发器、缓存键或其他服务鉴权行为。网关新增过滤器需正确处理 StripPrefix 后路径，且不能影响管理接口。
- 只升级异常类型与携带的 ResultCode；不改变五字段文本成功响应、Server 现有异常捕获行为或共享 Web 异常处理器。不声称错误码已通过 Feign 端到端传输。
- 保留已有构造方法及默认系统错误码，避免旧调用方编译不兼容；不修改缓存、预加载及数据库分配算法。

## Proof

- TinyIdGatewayIT：合法 HMAC 成功；普通有效用户、API Key/无 HMAC、错误/过期签名和无 URL 许可均无法到达下游；管理路径不受新增限制。
- TinyIdSegmentSecurityIT：匿名、伪造、过期 JWT 返回 401，合法 JWT 与旧 token 接口行为保持。
- TinyIdSysExceptionTest：BizException 继承、父类 code/msg 契约、默认 SYS_ERROR、自定义 ResultCode、旧构造方法的详情与 cause。
- 正式测试阶段先执行两仓目标测试，再运行相应全仓验证及变更文件规范检查；上一版测试报告不自动覆盖本次新代码。

## Implementation notes

- 已在两仓原任务分支实现，未触碰原 Cloud IAM 工作区。新增过滤器以路由 ID `macula-cloud-tinyid` 与 Spring PathPattern 识别 StripPrefix 后号段路径，order=199；不修改共享 JWT 签发/缓存或管理接口。
- 非 HMAC 拒绝用例模拟上游已认证安全上下文，不声称覆盖真实 IAM introspection；合法 HMAC、错误/过期签名和 URL 拒绝用例已串联新增过滤器。
- 异常复用 BizException 字段，无新字段或 ResultCode 实现；新增 `(ResultCode, String)` 与 `(ResultCode, String, Throwable)` 构造方法。四个原有构造方法保留。
- 已补充 Server 401 断言及异常单测，并同步 README；两仓 git diff --check 通过。尚未执行 Maven 编译、测试或本地安装，旧测试报告已标明不覆盖本次修改，原评审项等待验证和复审后关闭。
