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

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * <b>GlobalConstants</b> 安全使用的常量
 * </p>
 *
 * @author Rain
 * @since 2022-02-20
 */
public interface SecurityConstants {

    List<String> DEFAULT_IGNORE_URLS =
            Arrays.asList(
                    "/actuator/**", "/favicon*", "/webjars/**", "/doc.html", "/swagger-ui/**",
                    "/swagger-resources/**", "/v2/api-docs", "/v3/api-docs", "/h2", "/h2-console", "/h2/**",
                    "/h2-console/**", "/websocket", "/websocket/**", "/css/**", "/js/**", "/fonts/**",
                    "/images/**", "/static/**"
            );

    /**
     * 认证请求头key
     */
    String AUTHORIZATION_KEY = "Authorization";

    /**
     * JWT令牌前缀
     */
    String TOKEN_PREFIX = "Bearer ";

    String JWT_NICKNAME_KEY = "nickname";

    String JWT_DATASCOPE_KEY = "dataScope";

    String JWT_DEPTID_KEY = "deptId";

    /**
     * JWT存储权限前缀
     */
    String AUTHORITIES_PREFIX = "ROLE_";

    /**
     * 反向角色前缀
     */
    String NEGATED_ROLE_PREFIX = "!";

    /**
     * JWT存储权限属性
     */
    String AUTHORITIES_KEY = "authorities";

    /**
     * 根部门ID
     */
    Long ROOT_NODE_ID = 0L;

    /**
     * 系统默认密码
     */
    String DEFAULT_USER_PASSWORD = "123456";

    /**
     * 超级管理员角色编码
     */
    String ROOT_ROLE_CODE = "ROOT";

    String NOOP = "{noop}";

    String GRANT_TYPE_SMS = "sms";

    String BACKGROUND_USER = "*SYSADM";

    /**
     * 内部
     */
    String FROM_IN = "Y";

    /**
     * 标志
     */
    String FROM = "from";

    /**
     * 请求header
     */
    String HEADER_FROM_IN = FROM + "=" + FROM_IN;

}
