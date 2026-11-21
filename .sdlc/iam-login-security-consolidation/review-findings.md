# Plan review findings

## Plan — round 1
Reviewer: sdlc-reviewer fallback（codex-plan-review未安装），只读独立Plan pass；预算最多3轮，未运行代码Bugs/Security/Compliance审查。工作区未提交，round1基线副本/private/tmp/security-plan-round1.md；后续按该文件diff审查，不声称存在评审commit SHA。

- plan.md:75 — Important — 原Token Redis与SQL投影缺少线性化及原始expiry — 已补A：独立逐Token表、不可续期expiry、SQL提交/撤销优先、所有生命周期路径和失败注入。
- plan.md:76 — Important — System /test缺原Bearer至IAM的可信执行/结果合同 — 已补B：System初始化、同源原Bearer至IAM、IAM唯一写测试结果、System事务activate，绑定owner/jti/版本/摘要。
- plan.md:49 — Important — Admin缺MFA_REQUIRED消费者和System挑战获取入口 — 已补C：共享对话框与精确原Bearer用途端点、OTP/Passkey、并发/取消、原非幂等操作不自动重放。

上述为作者修正记录，尚待round2独立复审，不表示问题已由审查者关闭，也不表示人工plan已批准。

## Plan — round 2
只审/private/tmp/security-plan-round1.md到当前主计划diff，已读取round1记录；独立sdlc-reviewer反馈：未发现新增Important/Minor/Nit，round1三项在计划层面已闭环。未运行代码审查，不代表实现/测试通过，也未批准人工plan gate。实际共2轮，未超3轮预算。

作者补充：官方7.0.7 JAR以javap核对，Creation/Assertion仓库同名load返回不同类型，因此计划采用共享存储+两个局部适配器；Jackson2兼容限定WebAuthn局部。新增Boot AuthenticatedSecretCipher只为两服务共用标准JCA信封，不改变原资源@Passkey或CryptoManager合同。

配套说明：主计划及完整A/B/C修正位于相邻macula-cloud仓库同slug，本仓文件范围在plan.md。当前plan仍待人工批准。

后续状态澄清：上述“仍待人工批准”为round2结束时状态；用户后来明确YES批准，批准事实已记录在本仓及Cloud的plan.md。此次续做未重新批准任何gate，也未执行Stage 5代码审查。

## Build — 2026-11-20 整体只读检视

用户要求整体检视当前新代码。本次由当前会话按Bugs、Security、Compliance分别检查已实现增量及直接调用链，不是独立子代理审查，也不批准任何gate。未修改生产源码。Cloud实现范围参考实施前cloud.diff、未跟踪文件SHA256基线及当前文件，避免将其他任务的既有改动全部归入本slug。

### Important发现（待修复）

- `../macula-cloud/macula-cloud-iam/src/main/java/dev/macula/cloud/iam/security/protection/AccessTokenPublicationStore.java:109` — Important / P2 — 成功环境基线在发布事务提交前写入Redis。调用链为publish → publishDeviceAndSession → MaculaOAuth2AuthorizationService.issued → DefaultIamSecurityCoordinator.issued → confirmed/updateBaseline；后者立即写设备、国家/城市及成功IP基线。随后activate或事务commit失败时，SQL回滚且Redis授权被补偿删除，但风险基线没有回滚，未成功发布的环境会影响下次风险判定。修复：将这些成功副作用推迟至实际提交后，捕获不可变上下文；避免提交后通知失败反向触发已提交授权的补偿。补充activate/commit失败注入，断言成功基线保持原值。本项为源码调用链确认，未执行真实MySQL commit失败演练。
- `../macula-cloud/macula-cloud-iam/src/main/java/dev/macula/cloud/iam/security/DeviceSessionService.java:206` — Important / P2 — 仅以family==null区分完整登录和续接，导致直接grant的Refresh重复执行设备淘汰。DatabaseIamSecurityStateRepository.session仍把原loginFamilyId传给publish；直接grant及其refresh均没有浏览器family。由MULTI收紧为EXCLUSIVE/REPLACE_OLDEST或RECENT_IP后，刷新当前Token就可能removeDevice并撤销另一台设备；REJECT_NEW则可能拒绝本来应继续的刷新。修复：显式传递新主认证发布/续接语义，不以缺少浏览器family推断；增加真实refresh调用链回归。临时探针调用现有publish，固定已存在的直接grant会话，验证确实调用removeDevice(other-device)，不是生产环境操作。
- `../macula-cloud/macula-cloud-api/macula-cloud-system-api/src/main/java/dev/macula/cloud/system/security/protection/EffectiveLoginPolicy.java:76` — Important / P2 — 冲突检测使用legacy>0，漏掉旧deviceMaximum=0（不限）与已保存新EXCLUSIVE/有限MULTI规则的冲突。无DEVICE_SOURCE选择时会返回LEGACY/MULTI/maximum=0且conflicts为空，requireCompatible正常通过，管理员看不到需要选择来源，实际仍不限设备。修复：按旧键是否存在比较规则完整语义，零也作为有效配置值；覆盖零值冲突及零值等价的两条路径。临时探针实际输出legacyZeroConflict=[] effective=MULTI maximum=0，复现确认。

### 验证与边界

实际执行：

```bash
mvn -pl macula-cloud-iam -am test -Plocal -Dtest=MfaFreshnessServiceTest,MfaPolicyServiceTest,IamSecurityCoordinatorTest,DeviceSessionServiceTest,OAuth2PublicationAdapterTest,MaculaOAuth2AuthorizationServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

76项通过，失败0、错误0、跳过0；其中现有部分Test启动隔离Redis，不能统称纯单元证据。持久化测试中的AccessTokenPublicationStore被内存替身替换，未证明真实SQL事务提交、回滚或跨存储故障。当前未发现AccessTokenPublicationStoreIT/AuthenticationEvidenceStoreIT；需按accepted plan补齐后再验收。

两个临时Java探针使用本地已编译类及Mockito，仅在/private/tmp生成文件，最终运行成功；未写业务库、使用真实用户或调用云供应商。两仓git diff --check通过。

Boot本轮未发现新增证据充分的Important问题；Crypto信封与Passkey请求/响应边界已核对。供应商管理、WebAuthn端到端及生产RP仍未完成，不把已知未完成部分伪装成本轮新发现。Compliance仅按仓库已声明的秘密保护、身份/权限及审计规则检查，没有补造外部监管要求；除上述成功副作用时点问题外，本次未另报独立合规缺陷。

### 后续修复状态

用户回复OK后，已在Cloud修正上述三项：基线使用不可变发布快照并在afterCommit执行，提交后缓存异常不传播至原Token补偿；直接grant刷新显式使用原授权自身家族；旧deviceMaximum=0纳入冲突比较。修复均在既有accepted plan文件范围内，没有新增配置、迁移或信任来源。

先运行新增用例，27项中4个预期断言失败；修正后扩大到IAM/System API 88项与System策略4项，合计92项通过，失败/错误/跳过均0。事务回归使用Spring事务管理器实际同步生命周期与内存底层资源，覆盖正常提交、业务回滚、commit异常、提交后缓存故障及不可变环境快照；没有冒充真实MySQL故障演练。变更Java规范检查0违规。

该状态是作者修复与开发测试证据，不是独立Stage 5复审、整体需求完成或人工gate批准。Cloud主记录见相邻仓库同slug的build-notes.md和review-findings.md。真实MySQL故障验证与供应商/WebAuthn端到端仍待后续实施。


## Plan — 风险处置增量 round 1

Reviewer：sdlc-reviewer回退，配置的codex-plan-review未安装。只读独立Plan pass，最多3轮，本轮未执行代码审查/测试、未批准人工gate。round1基线保存在/private/tmp/risk-response-plan-round1.md。

- risk-response-plan.md:20 — Important — 预览基线缺服务端绑定合同 — 已补owner/认证会话、草稿/旧源/有效版本、摘要、TTL及一次性消费，旧源变化要求重建草稿/重新预览；待round2独立复审。
- risk-response-plan.md:31 — Important — 核验到证据写入缺同一不可变策略上下文 — 已补PolicyEvaluationContext及SQL策略/租户风险/账号锁序围栏，禁止record按最新策略重标旧验证；待round2独立复审。
- risk-response-plan.md:20 — Important — 可用因素预检不足以证明独立主认证路线 — 已补统一能力模型、分页可信投影、共同规则及SMS-only/SMS+EMAIL/PASSWORD+OTP/UV矩阵；待round2独立复审。
- risk-response-plan.md:61 — Important — 混跑时禁止activate缺可执行部署门禁 — 已补默认关闭的服务端部署开关、只读全实例核验脚本/清单及缺资料拒绝，开启部署变量仍须另行授权；待round2独立复审。
- risk-response-plan.md:20 — Minor — ID/版本精度合同缺失 — 已补字符串传输与大整数CAS往返测试；待round2独立复审。


## Plan — 风险处置增量 round 2

独立sdlc-reviewer仅审/private/tmp/risk-response-plan-round1.md到当前risk-response-plan.md的diff，并读取round1记录。反馈：无新增Important/Minor/Nit；round1四类Important与精度Minor在计划层面闭环。实际共2轮，未超3轮预算。未修改源码、执行测试或批准人工gate，不能视为实现/测试通过。

设计增量已由用户明确“确认”接受；risk-response-spec.md现为accepted。risk-response-plan.md保持draft，等待工程师对具体实施文件、API、配置与切换合同、风险和Proof作最终批准。原有镜像/真实迁移待确认状态不变。


## 用户范围修正：因素配置修订2

用户明确“不用保留旧配置”，并要求MFA只管理另外因素的启用规则和短信/邮件/Passkey具体配置。此前兼容方案及其两轮Plan结论仅为历史，不覆盖修订2；修订2删除legacy源、映射、双读、旧接口写入和MFA强制触发设置。新方案尚未独立审查/获批，不写源码或清理真实库。保留证据上下文、因素能力预检、CAS/精度和权限边界原则。


## Plan — 因素配置修订2 round 1

这是用户主动改写范围后的新方案第1轮，独立sdlc-reviewer只读Plan pass；新方案预算最多3轮，前版实际2轮作为历史。发现3类Important：首次新有效配置/部署门禁的启动闭环、渠道/模板关联是否真正可编辑生效、退役旧模型/Mapper时测试清单遗漏。已明确V38新默认active_version=1与停机前离线只读预检；MFA渠道/模板只读展示并复用原入口；补测试支持/历史迁移测试改写文件。待修订2 round2复审，不是实现/测试通过或人工批准。


## Plan — 因素配置修订2 round 2 及后续范围变化

独立sdlc-reviewer仅审修订2 round1基线至修订2修正diff，反馈无新增Important/Minor/Nit，三类计划问题已闭环。本修订实际2轮，旧方案也实际2轮，未改代码/执行测试/批准人工gate。随后用户新增TOTP及跨平台自助WebAuthn，新增范围详totp-webauthn-scope.md，需要另补完整实施合同后审查，不能借用前述审查宣称TOTP已通过。


## Plan — 最终四因素方案 round 1

用户追加TOTP、用户选择、分级allowedFactors/minimumStrength并确认UV Passkey后，此为最终新增合同的第1轮只读独立Plan审查（预算最多3轮）。前两个用户修订方案各实际2轮，作为历史不挪用。发现2类Important：
- risk-response-plan.md:38 — Important — owner/challenge预算不足以防跨challenge/Session重开TOTP猜码，登记confirm预算未冻结 — 已补同槽原子owner/credential/pending滚动预算、TTL、前置预扣/不退费、创建预算及并发/跨来源测试，待round2复审。
- risk-response-plan.md:32 — Important — pending登记缺原认证会话/应用/用途/安全版本/策略持久绑定 — 已补服务器EnrollmentContext及confirm/取消/替换锁内复核、原会话撤销/策略变化拒绝和替换竞争，待round2复审。
审查未修改源码、执行测试或批准人工gate，不能据此宣称任何新因素已实现。


## Plan — 最终四因素方案 round 2

独立sdlc-reviewer仅审/private/tmp/factor-plan-final-round1.md至本次修正的diff，已读取round1记录。反馈无新增Important/Minor/Nit，跨挑战预算与pending持久上下文两类Important已在计划层面闭环。本方案实际2轮，未超3轮预算；此前两个用户修订方案各实际2轮作为历史保留。没有修改源码、执行测试或批准人工gate。

最终risk-response-plan.md已冻结EMAIL/SMS/TOTP/PASSKEY、用户合格集合内选择及UV Passkey直接满足、依赖/迁移/端点/文件与Proof。计划保持draft等待工程师实施批准，不能把用户对UV Passkey规则的确认当作对尚未展示的最终工程变更清单批准。真实环境迁移/启用/容器切换继续暂停。


## Plan：安全菜单与按用户密码策略增量（2026-11-21）

Reviewer：既有sdlc-reviewer子代理sdlc_reviewer_risk_plan。codex-plan-review CLI不可用，依sdlc-build回退；仅Plan pass，最多3轮，不运行Bugs/Security/Compliance代码审查，不自行批准门禁。

### Round 1

- security-settings-plan.md:52 — Important — 遗留密码默认标准化未完整覆盖IAM仅status=1与System全行的实际语义。修订：分别按两端读取/默认/校验解析，逐上下文及跨租户证明一致，包含无字典及空租户场景；任何分歧在DDL前拒绝，并补禁用行用例。
- security-settings-plan.md:57 — Important — 默认事务isolation元数据及无序recorded遍历不能证明围栏和锁序成立。修订：所有相关外层显式RR，低隔离既有事务拒绝，新Mapper按identity排序当前读锁定后再锁sys_user，recorded复用已锁列表。
- security-settings-plan.md:58 — Important — 遗漏DefaultClientAccountDetailsService.updatePassword框架编码升级入口。修订：精确加入实现/测试/本地加载路径；真实本地来源及userId+凭据代际+预期旧哈希CAS，失败终止认证；编码升级不改密码年龄/历史/安全版本，实际改密推进独立代际，最终发布复核原代际，防止旧密码验证被重标新安全状态。

Round 1实际仅执行1轮，未改源码/运行测试；上述三类已修正，待round 2审查改动。不把计划审查结果计为实现/测试通过。


### Round 2

仅审security-settings-plan.md相对本次84行原草稿的精确差异，未混用此前主任务方案。无新增Important/Minor/Nit；round 1三类修正在计划层面闭环。本地来源证明约束覆盖全部IAM改密、恢复、编码升级及最终发布，不凭employee/同名/同号推断sys_user。

本增量累计实际2轮；子代理未修改文件/执行测试/批准人工gate。具体计划仍为draft，待工程师批准；原产品方向确认与已有四因素实施授权不被撤销或冒用为本增量工程审批。
