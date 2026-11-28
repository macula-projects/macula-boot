/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.constants;

/**
 * {@code CacheConstants} 缓存KEY的常量
 *
 * @author rain
 * @since 2023/4/10 23:41
 */
public interface CacheConstants {
    /** 用户与按钮权限缓存KEY前缀 */
    String SECURITY_USER_BTN_PERMS_KEY = "macula:cloud:system:user_btn_perms:";

    /** URL与角色关系缓存KEY [{接口路径:[角色编码]},...] */
    String SECURITY_URL_PERM_ROLES_KEY = "macula:cloud:system:url_perm_roles";

    /** 用户对应的应用缓存前缀 */
    String SECURITY_SYSTEM_APPS = "macula:cloud:system:apps:";

    /** 租户编码到租户 ID 的映射，供网关按应用名前缀识别归属。 */
    String SECURITY_SYSTEM_TENANTS = "macula:cloud:system:tenants";

    /** 应用缓存里面的密钥KEY */
    String SECURITY_SYSTEM_APPS_SECRIT_KEY = "secretKey";

    /** 应用认证后的业务租户，与应用归属租户分别维护。 */
    String SECURITY_SYSTEM_APPS_AUTH_TENANT_ID = "authTenantId";

    /** 按归属租户 ID 和应用 ID 发布的接口授权快照。 */
    String APPLICATION_PERMISSIONS_CACHE_PREFIX = "macula:cloud:system:app_perms:";

    /** 应用及 API Key 缓存中的来源 IP/CIDR 白名单，空值表示不限。 */
    String SECURITY_SYSTEM_APPS_PERMIT_IPS = "permitIps";

    /** System 发布的 API Key 身份缓存前缀。 */
    String APIKEY_CACHE_PREFIX = "macula:cloud:system:application:apikey:";

    /** API Key 缓存中记录应用编码的字段。 */
    String APIKEY_APPLICATION_CODE_ATTRIBUTE = "applicationCode";

    /** HMAC 与 API Key 凭据缓存共用的应用 ID 字段，也用于 API Key 的 JWT。 */
    String APIKEY_APPLICATION_ID_ATTRIBUTE = "applicationId";

    /** IAM 与其他服务共用的 Redis 键命名空间。 */
    String IAM_REDIS_PREFIX = "macula:cloud:iam:";

    /** IAM 资源服务器专用回调凭据，按 resource client_id 发布，不能用于普通业务 API。 */
    String IAM_CALLBACK_CREDENTIAL_PREFIX = IAM_REDIS_PREFIX + "resource:callback:";

    /** 回调凭据缓存中的 SHA-256 摘要。 */
    String IAM_CALLBACK_KEY_DIGEST = "apiKeyDigest";

    /** 仅允许已登记的回调路径。 */
    String IAM_CALLBACK_PATH = "path";

    /** oauth 客户端信息 */
    String OAUTH2_CLIENT_CACHE_KEY = IAM_REDIS_PREFIX + "oauth2:client";

    /** 授权同意状态 */
    String OAUTH2_CONSENT_KEY = IAM_REDIS_PREFIX + "oauth2:consent:";

    /** OAuth2 Token保存 */
    String OAUTH2_TOKEN_KEY = IAM_REDIS_PREFIX + "oauth2:token";

    /** 小程序登录的SESSION-KEY 保存 */
    String OAUTH2_TOKEN_WEAPP_SESSION_KEY = IAM_REDIS_PREFIX + "oauth2:session-key";

    /** 网关生成的JWT缓存 */
    String GATEWAY_JWT_CACHE_KEY = "macula:cloud:gateway:jwt:";

    /** 网关TOKEN的introspect信息缓存 */
    String GATEWAY_TOKEN_CACHE_KEY = "macula:cloud:gateway:principal:";

    /** 网关签名防重放的 Redis 键前缀。 */
    String GATEWAY_NONCE_KEY = "macula:cloud:gateway:nonce:";

    /** WEBSOCKET订阅关系KEY */
    String WEBSOCKET_USERS_KEY = "macula:ws:u:user:";

    /** WEBSOCKET订阅关系会话KEY */
    String WEBSOCKET_USER_SESSIONS_KEY = "macula:ws:u:sessions:";

    /** WEBSOCKET订阅关系会话与用户对应KEY */
    String WEBSOCKET_SESSION_USER_KEY = "macula:ws:s:user:";

    /** WEBSOCKET订阅关系会话订阅KEY */
    String WEBSOCKET_SESSION_SUBSCRIPTIONS_KEY = "macula:ws:s:subs:";
}
