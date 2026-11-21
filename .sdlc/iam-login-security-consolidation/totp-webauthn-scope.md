# 认证因素范围补充：EMAIL / SMS / TOTP / PASSKEY
Status: accepted

用户明确要求邮箱、SMS、类似Authy的动态口令，以及用户可启用、覆盖Apple/Windows/Android的WebAuthn。本要求覆盖此前“不新增TOTP”的Non-goal，不是旧配置恢复、源码实施批准或运行环境切换授权。

## 因素与配置职责
- EMAIL：已绑定邮箱验证码，复用邮件投递渠道；SMS：已绑定手机验证码，复用短信投递渠道。两者沿用既有供应商秘密、渠道权限及唯一投递/预算，不另存一份供应商配置。
- TOTP：标准RFC6238验证器动态口令，支持Authy这类通用验证器；本地服务器验证，不要求Authy云服务、推送批准或Twilio账号。初期互通配置固定HMAC-SHA1、6位、30秒，漂移至多前后1步，参数写入登记记录/otpauth URI，不提供任意算法降级或无限窗口。
- PASSKEY：使用WebAuthn注册/验证，用户选择自己设备或密码管理器提供的认证器。目标浏览器场景覆盖Apple iOS/macOS、Windows Hello、Android支持环境；具体浏览器/设备以能力检测及真实验收矩阵为准，不把虚拟认证器通过当作全平台实机完成。不把OS厂商建成三套登录协议；不采集指纹、人脸或设备解锁PIN。

平台MFA页为四类因素配置启用/适用主认证方式/场景/用户范围、独立组合、OTP参数或UV要求及真实可用状态。用户个人安全中心管理实际绑定；WebAuthn可由用户主动注册启用，平台开放不等于自动为用户登记凭据。风险/场景策略仍负责何时必须认证和最低强度，MFA不重新评分。

## TOTP生命周期与证据边界
1. 新增本人TOTP绑定时做owner、CSRF及近期身份复核，不能只凭被盗Session新增因素；复用主任务的注册/恢复授权边界，不新增匿名重置入口。
2. 服务端生成密码学安全随机seed，生成otpauth URI及本地二维码，pending绑定短期、owner/用途/策略绑定；扫码后必须输入真实动态码确认，未确认不能计为可用因素。二维码/seed只在受控登记阶段展示，不调用外部二维码服务，不在HTTP URL/query、日志、审计或已启用凭据列表中出现；标准otpauth登记URI内的secret仅作为一次性受控响应/本地二维码材料。
3. seed必须可用于服务端核验，因此使用现有AuthenticatedSecretCipher的AES-GCM信封保护，独立部署keyring、用途AAD/keyId；不能仅哈希seed，也不能明文存DB/Redis。缺密钥拒绝登记/核验，不使用弱替代。
4. 服务端校验并原子推进credential的lastAcceptedTimeStep；同一步码不能因并发、切换challenge、登录/敏感用途而多次生成成果。失败次数/限频有界，时钟漂移受限。核验、策略围栏、账号版本、凭据状态、证据提交遵守已定义的同一主库事务边界。
5. 用户可查看绑定状态、命名、停用/重绑；停用或重置推进安全版本/撤销受影响成果，不能让旧码继续使用。最后可用强因素移除需先完成替代绑定或既有受控恢复；不以管理员删除按钮绕过。
6. PASSWORD+TOTP可满足独立MFA；SMS/EMAIL/TOTP本身都不能仅因用了两个通道/两个验证器就自动当成独立因素。UV Passkey可按服务器验证结果满足强认证，足够时不再机械追加短信。TOTP是AuthenticationFactor，不加入SMS/MAIL投递Channel，也没有send-code动作。

## 平台支持与验证
- TOTP：RFC测试向量、互通6位格式、前导零、漂移边界、过期、重放/并发、跨owner/用途/策略拒绝、密文篡改/缺密钥、绑定确认/撤销及不泄密。
- WebAuthn：Apple Safari/iOS/macOS、Windows Edge/Chrome+Hello、Android Chrome等实际支持组合逐一记录注册、登录、MFA/敏感补验、撤销/恢复结果；不可用环境提供其他已绑定因素，不伪造降级通过。HTTPS、显式可信RP ID/allowedOrigins和UV校验仍按主计划执行；正式域名未提供前只做显式本地环境验收。
- 全部因素共用风险处置、owner/应用/认证会话/安全版本、近期证据与策略上下文；生物信息和私钥留在认证器，服务器仅保存WebAuthn公钥凭据及状态。

## 实施影响（计划仍为draft）
- API AuthenticationFactor追加TOTP，因素启用/能力预检/独立组合与证据模型纳入TOTP，短信投递Channel不变。
- IAM新增TOTP登记/确认/撤销与核验服务、受保护seed存储及lastAcceptedTimeStep原子更新；System追加下一可用迁移（当前预留V39__iam_totp_credentials.sql，落地前复查）。个人安全中心及MFA挑战页面增加验证器绑定和动态码输入，不出现发送动态口令按钮。
- Gateway MfaFactor追加TOTP；固定控制路径追加totp/verify，仅接收challenge id和code，并沿用原Bearer恢复。浏览器登录挑战追加受CSRF保护的totp/verify，不混用短信/邮件发码接口。
- 复用当前BOM Hutool 5.8.47的cn.hutool.crypto.digest.otp.TOTP（已javap核对当前JAR构造器/generate接口），不手写HMAC/截断算法。二维码依赖已在最终计划冻结为ZXing core/javase3.5.3，由Boot父BOM管理、IAM按需引用；精确POM/源码/测试清单已完成2轮独立Plan审查，仍待工程师实施批准；不因为算法库存在就宣称防重放/绑定/恢复已实现。
- 上一版因素修订2实际完成2轮Plan审查，round2无新增发现；该结论不覆盖此次新增TOTP。新范围的完整文件/依赖/迁移/契约及验证计划现已合并并完成独立审查，详risk-response-plan.md及review-findings.md，仍待实施批准。

参考：RFC6238 https://www.rfc-editor.org/rfc/rfc6238；Authy/TOTP说明 https://www.twilio.com/docs/verify/totp；Apple https://developer.apple.com/documentation/authenticationservices/supporting-passkeys/；Windows https://learn.microsoft.com/en-us/windows/security/identity-protection/passkeys/；Android/Chrome https://developers.google.com/identity/passkeys/supported-environments。这些来源证明技术合同，不是本项目已实现或真实设备验收证据。


## 用户选择与后台风险规则（用户后续明确要求）

用户要求“MFA方式用户应该可以选择”，并补充“后台启用使用哪几种，什么风险等级要哪种”。两者组合为后台约束、用户在合格集合内选择，不做二选一的产品限制。

1. 平台因素配置决定EMAIL/SMS/TOTP/PASSKEY哪些可启用，以及适用主认证方式/场景/目录与独立组合规则。管理员停用因素不删除用户已有绑定或秘密，但停用后不能用于新的认证成果。
2. 风险/场景处置在LOW/MEDIUM/HIGH各档配置动作、allowedFactors、minimumStrength和证据时限；可要求某一种、允许几种供选择，或BLOCK拒绝本次操作。指定PASSKEY时仍验证UV，不能只以方式名当强度证明。配置不自动激活或改写真实环境。
3. 本次可选集合=平台启用且适用的因素∩该风险/场景允许因素∩用户已绑定有效因素∩当前实际可用因素，再筛除不满足主认证独立性、UV或最低强度的组合。空列表不表示全部，无可选因素时明确拒绝/引导既有受控恢复，不自动放宽。
4. 挑战接口只返回该owner本次真正可选的方式；多种时提供选择/切换，只有一种时直接展示该方式。用户不能选择策略禁用的方式，也不能用自报factor、绑定信息或前端选项跳过后端复核。
5. EMAIL/SMS显示发送验证码，TOTP显示动态码输入且没有发送按钮，PASSKEY调用浏览器认证器。切换仍绑定原owner、应用、challenge、策略版本和总体尝试预算，不重建挑战扩充次数，不自动重放原业务写请求。
6. 风险allowedFactors及minimumStrength纳入不可变PolicyEvaluationContext、挑战和有效策略摘要；保存/激活预检计算风险等级对应的真实可用集合与操作者自锁风险，不能只统计账号拥有任意一个因素。
7. 测试覆盖：多方式自主选择/切换、单方式限定、平台禁用、风险限定、未绑定、设施/浏览器不支持、强度不足、交集为空、跨owner/旧策略拒绝；证明前端可选项与后端实际允许结果一致。该补充是用户直接明确的需求，具体实现计划仍待补审与批准。

示例仅说明可配置能力，不是已启用默认值：低风险不追加认证；中风险允许密码主认证后的EMAIL/SMS/TOTP或满足要求的UV Passkey；高风险限定UV Passkey，或由管理员配置BLOCK。账号硬拒绝、个人/接口必须认证及业务权限约束仍独立。


## 用户最终确认：UV Passkey与MFA

用户明确“OK，确认”接受：当前策略允许PASSKEY，且服务器已验证UV、owner/应用/设备/认证会话/安全版本/环境与新鲜度均满足时，Passkey主登录或密码后的Passkey补验都可直接满足强认证，无需机械追加短信或再次Passkey。UP或签名成功但无UV不能作为该强度证明；不能以“更强”绕过BLOCK、账号锁定、业务权限或策略明确限定的方法。本确认已纳入最终实施计划，但不自动批准新增文件/迁移或真实运行切换。
