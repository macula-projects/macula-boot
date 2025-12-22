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

package dev.macula.boot.starter.mp.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.enums.DataScopeEnum;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.util.ClassUtils;

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
        if (ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder",
            SecurityUtils.class.getClassLoader())) {
            if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext()
                .getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication()
                .isAuthenticated()) {
                return SecurityContextHolder.getContext().getAuthentication().getName();
            }
        }
        return null;
    }

    /**
     * 获取用户角色
     *
     * @return 角色 Code集合
     */
    public static Set<String> getRoles() {
        if (ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder",
            SecurityUtils.class.getClassLoader())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && CollectionUtil.isNotEmpty(authentication.getAuthorities())) {
                return authentication.getAuthorities().stream()
                    .map(item -> StrUtil.removePrefix(item.getAuthority(), "ROLE_")).collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    /**
     * 获取Token 属性
     *
     * @return Token属性 Map
     */
    public static Map<String, Object> getTokenAttributes() {
        if (ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder",
            SecurityUtils.class.getClassLoader()) && ClassUtils.isPresent(
            "org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken",
            SecurityUtils.class.getClassLoader())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                if (authentication instanceof AbstractOAuth2TokenAuthenticationToken<?> tokenAuthenticationToken) {
                    return tokenAuthenticationToken.getTokenAttributes();
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * 获取部门 ID
     *
     * @return deptId
     */
    public static Long getDeptId() {
        return Convert.toLong(getTokenAttributes().get(SecurityConstants.JWT_DEPTID_KEY), SecurityConstants.ROOT_NODE_ID);
    }

    /**
     * 获取数据权限
     *
     * @return DataScope
     */
    public static Integer getDataScope() {
        return Convert.toInt(getTokenAttributes().get(SecurityConstants.JWT_DATASCOPE_KEY), DataScopeEnum.DEFAULT.getValue());
    }

    /**
     * 获取当前租户 ID
     *
     * @return 租户 ID
     */
    public static Long getTenantId() {
        return Convert.toLong(getTokenAttributes().get(GlobalConstants.TENANT_ID_NAME), GlobalConstants.DEFAULT_TENANT_ID);
    }

    /**
     * 判断用户是否为超级管理员
     *
     * @return true/false
     */
    public static boolean isRoot() {
        return getRoles().contains(SecurityConstants.ROOT_ROLE_CODE);
    }
}
