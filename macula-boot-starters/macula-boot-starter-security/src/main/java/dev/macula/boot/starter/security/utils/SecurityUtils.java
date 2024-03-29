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

package dev.macula.boot.starter.security.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code SecurityUtils} 安全助手
 *
 * @author rain
 * @since 2022/7/25 15:16
 */
public class SecurityUtils {

    public static String getCurrentUser() {
        if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext()
            .getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return null;
    }

    /**
     * 获取用户昵称/姓名
     *
     * @return nickname
     */
    public static String getNickname() {
        return Convert.toStr(getTokenAttributes().get(SecurityConstants.JWT_NICKNAME_KEY));
    }

    /**
     * 获取用户角色
     *
     * @return 角色Code集合
     */
    public static Set<String> getRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && CollectionUtil.isNotEmpty(authentication.getAuthorities())) {
            return authentication.getAuthorities().stream()
                .map(item -> StrUtil.removePrefix(item.getAuthority(), "ROLE_")).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * 获取部门ID
     *
     * @return deptId
     */
    public static Long getDeptId() {
        return Convert.toLong(getTokenAttributes().get(SecurityConstants.JWT_DEPTID_KEY));
    }

    /**
     * 获取数据权限
     *
     * @return DataScope
     */
    public static Integer getDataScope() {
        return Convert.toInt(getTokenAttributes().get(SecurityConstants.JWT_DATASCOPE_KEY));
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        return Convert.toLong(getTokenAttributes().get(GlobalConstants.TENANT_ID_NAME));
    }

    public static String getTokenId() {
        if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext()
            .getAuthentication() instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken token =
                (JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
            if (token != null && token.getToken() != null) {
                return token.getToken().getId();
            }
        }
        return null;
    }

    /**
     * 判断用户是否为超级管理员
     *
     * @return true/false
     */
    public static boolean isRoot() {
        return getRoles().contains(SecurityConstants.ROOT_ROLE_CODE);
    }

    public static Map<String, Object> getTokenAttributes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication instanceof AbstractOAuth2TokenAuthenticationToken) {
                AbstractOAuth2TokenAuthenticationToken<?> tokenAuthenticationToken =
                    (AbstractOAuth2TokenAuthenticationToken<?>)authentication;
                return tokenAuthenticationToken.getTokenAttributes();
            }
        }
        return Collections.emptyMap();
    }
}
