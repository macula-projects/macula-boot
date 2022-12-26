/*
 * Copyright (c) 2022 Macula
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

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * <b>SecurityConstants</b> 安全相关常量
 * </p>
 *
 * @author Rain
 * @since 2022-02-20
 */
public interface SecurityConstants {

    List<String> DEFAULT_IGNORE_URLS = Arrays.asList("/favicon**", "/webjars/**", "/doc.html", "/swagger-ui/**", "/swagger-resources/**", "/v2/api-docs", "/v3/api-docs");

    /**
     * 认证请求头key
     */
    String AUTHORIZATION_KEY = "Authorization";

    /**
     * JWT令牌前缀
     */
    String TOKEN_PREFIX = "Bearer ";


    /**
     * Basic认证前缀
     */
    String BASIC_PREFIX = "Basic ";

    /**
     * JWT载体key
     */
    String JWT_PAYLOAD_KEY = "payload";

    /**
     * JWT ID 唯一标识
     */
    String JWT_JTI = "jti";

    /**
     * JWT ID 唯一标识
     */
    String JWT_EXP = "exp";

    /**
     * 黑名单token前缀
     */
    String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";

    String USER_ID_KEY = "userId";

    String USER_NAME_KEY = "username";

    String CLIENT_ID_KEY = "client_id";

    /**
     * JWT存储权限前缀
     */
    String AUTHORITIES_PREFIX = "ROLE_";

    /**
     * JWT存储权限属性
     */
    String AUTHORITIES_KEY = "authorities";


}
