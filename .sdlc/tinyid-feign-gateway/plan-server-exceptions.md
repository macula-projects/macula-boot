# Plan: TinyID Server 接入统一异常链路（from spec.md）

Status: accepted。用户已明确回复 YES。本补充替代原计划中“保持新号段接口异常捕获行为不变”的约束，原有三个 token 接口暂不改契约。

## Files that change

## Controller 简化修订（已接受）

用户要求 Controller 不声明 Result，无需特殊恢复时不捕获异常；特殊异常转换放在 Service。用户 YES 已批准本修订，替代原“旧三个接口错误契约不变”的约束，前一版实施记录保留为历史。

### Files that change

- Cloud TinyID 新增 `service/TinyIdIssuingService.java`、`service/impl/TinyIdIssuingServiceImpl.java` 及对应单测，集中承接发号编排、旧 token 校验与批量上限规则。
- 修改 `IdContronller.java`、`IdContronllerTest.java`、`TinyIdExceptionIT.java`、`TinyIdSegmentSecurityIT.java`、README 及两仓任务记录；必要的异常转换复用现有 TinyIdSysException，不改共享 Advice。

### Order of work

1. Service 承接 token 校验及发号调用。token 不允许时抛 TinyIdSysException(TOKEN_ERR, 安全详情)；已有业务异常原样传播，仅在确需转换底层失败时封装安全消息并保留 cause，不机械地捕获所有异常。
2. Controller 删除所有 try/catch、错误日志、Result.success/failed；nextId 返回 List<Long>，nextSegmentId 返回 SegmentId，由 ControllerResponseAdvice 统一包装普通请求，Feign 请求按统一规则直接返回数据。
3. 两个 Simple 接口保留文本成功协议（新号段接口 ResponseEntity<String> 仅用于声明 text/plain，不是业务 Result 包装）；文本格式化属于协议适配，可留在 Controller。所有业务失败交给 ControllerExceptionAdvice。
4. 保留 token 校验规则、批量上限及本地号段算法；不顺带重命名现有 IdContronller 类型。
5. 更新 Service/Controller/Web 测试和迁移说明，完成后再确认进入正式验证。

### Risks

- 旧接口不再返回 HTTP 200 错误 Result 或空串，业务失败统一 HTTP 500 Result JSON；旧 token 校验本身不删除。
- nextId/nextSegmentId 普通请求保持统一成功包装；有 Feign 标记时按项目约定返回原始业务数据，需要明确告知旧调用方。

### Proof

- Service 覆盖授权拒绝不发号、批量上限、业务异常传播及特殊转换的 code/cause。
- Controller 用例断言业务数据返回和无异常吞没；Web IT 覆盖统一成功/失败响应、有无 Feign 标记、文本成功和 token 拒绝。
- 既有 HMAC、JWT 与号段缓存用例继续回归，未运行测试前不宣称通过。

## 原已接受计划文件范围

## 管理接口与前端修订（已接受，与 Controller 简化修订一并实施）

用户明确要求 TinyIdAdminController 不使用 NotControllerResponseAdvice，直接由统一 Advice 返回 Result，管理前端参考 system 调整。本修订替代下方历史实施中的管理原始响应兼容方案。

用户已明确回复 YES，批准本修订与 Controller 简化一起实施。

### Files that change

- Cloud TinyID `controller/TinyIdAdminController.java`、`TinyIdExceptionIT.java`、README。
- Cloud Admin `src/views/tinyid/management/` 下 ApplicationPanel、BusinessPanel、AuditLogPanel、ApplicationCreateDialog、ApplicationRemarkDialog、ApplicationAuthorizationDialog、BusinessCreateDialog；按实际数据消费核对 index.vue。
- 前端 `tests/unit/tinyid-management.test.js`、`cypress/e2e/tinyid-management.cy.js` 及关联 fixture（如有）、README/任务记录。`src/api/model/tinyid/management.js` 沿用现有共享 http 调用，不新建解包协议；共享 request.js 不改。

### Order of work

1. 移除 TinyIdAdminController 全部 NotControllerResponseAdvice 及其 import，Controller 返回业务对象，Advice 统一包装普通管理请求。
2. 参考 system，页面先检查 response.success，再从 response.data 读取列表、分页、详情、一致性和创建返回的 token；失败显示 cause/msg，不兼容双重响应结构。
3. 保存、授权、备注更新及删除等写操作仅在 success=true 时提示成功、关闭对话框或刷新；业务失败保持当前界面，网络失败释放 loading。共享请求层会正常返回含错误消息的 HTTP 500 响应，不能仅依赖 catch。
4. 更新后端响应断言、前端单测和 Cypress 模拟响应为统一 Result；验证列表、分页、创建、删除、错误提示及 loading 恢复。

### Risks

- 管理响应由原始对象变为统一 Result，需与前端协调升级。ROOT 授权、路由、业务字段及数据库不变。
- API 返回的创建 token 不写日志；错误不得触发成功状态。原 Cloud 工作区的未提交前端测试改动继续保留，本次只修改独立任务 worktree。
- 仍保留现有数值 JSON 类型配置，不在本修订顺带改变数值字段类型。

### Proof

- 后端 Web IT 验证普通管理请求只有一层 Result，统一异常和权限测试继续覆盖。
- 前端测试覆盖 success/data 解包、业务错误不关闭表单/不提示成功、网络错误释放 loading，以及分页、详情和 token 展示。
- 正式验证阶段执行相关 Vitest、前端 build、TinyID Cypress；模拟接口的 Cypress 明确标为模拟，不作为真实后端 E2E 证据。

## 原计划具体文件范围（历史）

Cloud 独立 worktree：
- `macula-cloud-tinyid/pom.xml`、README.md。
- `controller/IdContronller.java`、`service/impl/DbSegmentIdServiceImpl.java`、`pojo/vo/ErrorCode.java`（均位于该模块 Java 根包下）。
- `IdContronllerTest.java`、`TinyIdSegmentSecurityIT.java`，新增 `DbSegmentIdServiceImplTest.java` 和 `TinyIdExceptionIT.java`。

Boot TinyID 模块：
- `remote/TinyIdFeignClient.java`、`base/generator/impl/CachedIdGenerator.java`、README.md。
- `TinyIdFeignClientIT.java`、`CachedIdGeneratorTest.java`。
- 两仓该任务 spec/plan、verification/review 记录。

## Order of work

1. 仿照 system 引入 macula-boot-starter-web，复用 ControllerExceptionAdvice，不复制统一异常处理代码。核对响应包装对 TinyID 管理接口和旧文本接口的影响；旧接口使用必要的禁止包装标记维持原响应。
2. DbSegmentIdServiceImpl 使用明确 ResultCode 抛出 TinyIdSysException；ErrorCode 保留已有 ID500/ID502，并追加业务不存在、号段更新冲突的独立错误码。数据库查询、事务和重试分配算法不变。
3. 新号段接口移除吞异常后返回空串的逻辑，允许 TinyIdSysException 交给统一处理器，输出 HTTP 500 + Result JSON（沿用现有 BizException 策略）。成功仍返回五字段 text/plain，不改为远程 DTO。采用 ResponseEntity<String> 明确成功类型，避免统一包装。
4. 清理该接口的 text/plain-only 内容协商约束，并同步 Feign Accept，保证成功文本与失败 JSON 均可协商；保留必填/非空参数校验。旧三个接口的 token 校验、成功格式与错误契约不变。
5. 复用 OpenFeignErrorDecoder 还原 BizException。CachedIdGenerator 对已有 BizException 保留 code/msg 并包装为 TinyIdSysException，保留 cause；其他异常仍为默认系统错误，不添加重试或回退。不让远程结果码在本地发号入口丢失。
6. 同步 README 迁移说明和测试。完成实施确认后进入正式验证与复审，不提交、推送、部署。

## Risks

- 新号段接口的失败响应由 HTTP 200 空串变为 HTTP 500 Result JSON，是本次明确需要确认的协议变更；成功五字段文本保持。
- 引入统一 Web Advice 会影响整个 Server，必须核对并保护旧文本及管理 API 的既有响应，不能只验证新接口。
- 异常详情和 cause 与公开 code/msg 分开；不将凭据或底层 SQL 信息新增到公共错误消息中。
- 当前正式验证报告已过期，本补充也须实际测试，不能沿用旧版绿灯。原 Cloud IAM 工作区保持不动。

## Proof

- 服务单测：业务不存在和冲突耗尽返回各自错误码，成功号段与既有重试边界不变。
- Controller 单测与 Web IT：真实统一异常处理器输出 HTTP 500 JSON 及原始 code/msg；成功仍为 text/plain；缺失/空 bizType 和旧 token 接口回归。
- 开启生产 Web 自动配置检查管理 API 不发生重复包装，旧文本接口不被包装；请求有/无 Feign 标记均覆盖。
- Feign IT：真实本地 HTTP 的文本成功与错误 JSON 经现有 decoder 解码；CachedIdGenerator 回归验证错误码、消息、cause 保留及空响应默认系统错误。
- 正式测试阶段按目标模块到跨仓全量的顺序执行，另行报告实际结果及真实网关联调边界。

## Implementation notes

- 最新修订已实施：新增 TinyIdIssuingService/Impl 承接旧 Token 校验及批量约束，IdContronller 无 Result、try/catch 或日志，文本接口仅保留协议编码。旧 token 拒绝统一抛 ID500，普通 JSON 成功由 Advice 包装，Feign 标记请求按统一约定返回原始数据。
- 管理 Controller 已移除全部 NotControllerResponseAdvice；7 个管理组件检查 success 并读取 data，业务失败不关闭表单/不提示成功；共享 HTTP 层已提示的传输失败只终止当前操作，finally 释放 loading/saving。未改共享 request.js 或 API 解包协议。
- 为保持管理端错误响应一致，TinyIdHttpExceptionAdvice 在保留 MVC HTTP 状态的同时输出 Result JSON；不新增 Controller 内异常处理。覆盖 400/405 与管理错误的结构。
- 更新 Service/Controller/Web/Security 测试，补充实际挂载组件的失败/成功及传输错误状态测试；Cypress 模拟响应统一 Result，覆盖创建 token 和删除失败。index.vue 无数据消费，不需修改。
- 本轮仅实施及静态差异检查，未执行 Maven/npm/浏览器测试。下列原始响应兼容描述是已被最新修订替代的历史记录。

- 已按用户 YES 实施；原有两个计划中的“新号段接口捕获行为不变”由本补充明确替代。两仓均留在原任务分支，未操作原 Cloud IAM 工作区。
- 替换 TinyID Server 的 Spring Web 依赖为 Macula Web；新接口返回 ResponseEntity 文本成功，异常交给统一 BizException Advice。Feign Accept 包含 text/plain 和 application/json，CachedIdGenerator 保留远程 code/msg/cause，已是 TinyIdSysException 时直接传播。
- 为实现计划中的兼容约束，实际补充修改 TinyIdAdminController（各映射禁止响应包装）和 application.yml（long-to-string=false）；原构建 JAR 未包含 Macula Web，原管理接口是直接返回对象，需避免引入包装和数字转字符串。
- 新增 config/TinyIdHttpExceptionAdvice，复用 Spring ResponseEntityExceptionHandler 优先处理 MVC/显式 HTTP 异常，防止 400/404/405 等落入通用 Exception 处理器变成 500；不复制业务异常处理逻辑。
- ErrorCode 新增 ID503/ID504；存储意外异常包装为 ID502 和安全详情，底层原因只保留为异常 cause，事务及乐观锁重试逻辑不变。
- 新增 DbSegmentIdServiceImplTest、TinyIdExceptionIT；更新 Controller、安全链、Feign 和缓存异常用例。新 Web IT 导入生产 Web 自动配置，覆盖有/无 Feign 标记的成功文本、错误 JSON、管理原始响应、旧数字类型、旧 token 拒绝和 MVC 状态。业务边界为 mock，不冒充真实 E2E。
- 两仓 git diff --check 通过。未执行 Maven 编译、测试或本地安装；正式验证和复审待实施确认后进行。此前测试报告不能覆盖本次改动。
