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
 * {@code CacheConstants} is
 *
 * @author rain
 * @since 2023/4/10 23:41
 */
public interface CacheConstants {

    /** URL与角色关系缓存KEY [{接口路径:[角色编码]},...] */
    String SECURITY_URL_PERM_ROLES_KEY = "system:security:url_perm_roles";

    /** 用户与按钮权限缓存KEY前缀 */
    String SECURITY_USER_BTN_PERMS_KEY = "system:security:user_btn_perms:";

    /** 用户对应的应用缓存前缀 */
    String SECURITY_SYSTEM_APPS = "system:security:system_apps:";

    /** 应用缓存里面的密钥KEY */
    String SECURITY_SYSTEM_APPS_SECRIT_KEY = "secretKey";

    /** 应用缓存里面的允许URL KEY */
    String SECURITY_SYSTEM_APPS_PERMIT_URLS = "permitUrls";

    /** oauth 客户端信息 */
    String OAUTH2_CLIENT_KEY = "iam:oauth2:client";

    String OAUTH2_CONSENT_KEY = "iam:oauth2:consent:";

    String OAUTH2_TOKEN_KEY = "iam:oauth2:token";
}
