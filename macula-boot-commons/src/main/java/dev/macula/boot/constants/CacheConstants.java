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

    /** 应用缓存里面的密钥KEY */
    String SECURITY_SYSTEM_APPS_SECRIT_KEY = "secretKey";

    /** 应用缓存里面的允许URL KEY */
    String SECURITY_SYSTEM_APPS_PERMIT_URLS = "permitUrls";

    /** oauth 客户端信息 */
    String OAUTH2_CLIENT_CACHE_KEY = "cloud:iam:oauth2:client";

    /** 授权同意状态 */
    String OAUTH2_CONSENT_KEY = "macula:cloud:iam:oauth2:consent:";

    /** OAuth2 Token保存 */
    String OAUTH2_TOKEN_KEY = "macula:cloud:iam:oauth2:token";

    /** 小程序登录的SESSION-KEY 保存 */
    String OAUTH2_TOKEN_WEAPP_SESSION_KEY = "macula:cloud:iam:oauth2:session-key";

    /** 网关生成的JWT缓存 */
    String GATEWAY_JWT_CACHE_KEY = "macula:cloud:gateway:jwt:";

    /** 网关TOKEN的introspect信息缓存 */
    String GATEWAY_TOKEN_CACHE_KEY = "macula:cloud:gateway:principal:";

    /** 网关nonce参数信息缓存 */
    String GATEWAY_NONCE_KEY = "macula:scg:nonce";

    /** WEBSOCKET订阅关系KEY */
    String WEBSOCKET_USERS_KEY = "macula:ws:u:user:";
    /** WEBSOCKET订阅关系会话KEY */
    String WEBSOCKET_USER_SESSIONS_KEY = "macula:ws:u:sessions:";
    /** WEBSOCKET订阅关系会话与用户对应KEY */
    String WEBSOCKET_SESSION_USER_KEY = "macula:ws:s:user:";
    /** WEBSOCKET订阅关系会话订阅KEY */
    String WEBSOCKET_SESSION_SUBSCRIPTIONS_KEY = "macula:ws:s:subs:";
}
