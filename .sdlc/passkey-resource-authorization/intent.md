# Intent: PassKey 资源访问凭证
Author: 待确认. Status: accepted.

## Problem
列表已按业务权限过滤，但后续接口仍可能因资源编号被替换而访问其他资源，需要复用列表已经作出的资源授权。

Passkey 主要用于资源编号与当前用户的关系不明确、必须依靠前置授权列表框定范围的业务。数据已有明确用户归属时，直接使用 CheckOwner 的返回值检查，无需为此引入列表查询或凭证。

## Proposed outcome
列表记录携带绑定当前用户、全局唯一资源编号和有效期的 passkey。使用一个方法级 CheckOwner 注解：返回值表达式在执行后比较拥有者，入参表达式在执行前验证 Header 或 Query 携带的资源凭证。模式由表达式确定，不由客户端是否携带凭证决定。缺失、无效、过期、用户或资源不匹配时拒绝，入参模式下业务方法不执行。

列表与详情 DTO 可以不同，统一接口声明资源编号及 passkey 的访问方法；JSON 保留 passkey，隐藏辅助 resourceId。凭证不包含租户、类型、action；PMS 负责操作权限，业务负责实时状态等条件。业务保证列表可见即有资源访问权限。

## Affected users and systems
公共契约、Web Starter 的现有响应 Advice、Security Starter 的现有配置与方法鉴权，以及接入业务和前端。与 CheckOwner 整合为同一资源鉴权能力，复用配置与可共享的身份及表达式处理，保留 CheckOwner 和未接入接口行为。

## Constraints
Java 17 兼容；复用现有结构、配置与 Advice，不另建重复包装流程。服务端独占签名密钥，无默认密钥。业务明确保证所有返回 PasskeyResource 列表元素的接口都已完成当前用户的数据权限过滤；接口实现本身作为签发声明，无需签发注解。覆盖直接列表及 Result 包装，分页支持范围明确。拒绝 Header/Query 的重复和冲突。保留标准参数绑定及 PMS 校验。新能力可配置、可替换，但已声明受保护的方法不能静默放行。凭证有效期内可复用；不提供立即撤权、一次性消费或操作权限推导。

## Open questions
- 用户明确确认统一使用 CheckOwner：returnObject 表达式后置检查归属，入参表达式前置验证 passkey；混合两种来源的表达式拒绝。
- 用户已确认：移除 IssuePasskey，Advice 对已授权列表中的 PasskeyResource 元素自动签发。此类 DTO 不得用于未授权列表。
- 默认有效期建议 5 分钟，密钥轮换及分页支持范围待设计确认。
- 需求作者署名待确认。
