/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
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

package dev.macula.boot.starter.security.test;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.security.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.*;

/**
 * <p>
 * <b>SecurityUtilsTest</b> 安全工具类单元测试
 * </p>
 * <p>
 * 测试获取当前用户信息、角色、租户ID、部门ID等方法
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 测试没有认证信息时返回null
     */
    @Test
    void getCurrentUserReturnsNullWhenNotAuthenticated() {
        String currentUser = SecurityUtils.getCurrentUser();
        Assertions.assertNull(currentUser);
    }

    /**
     * 测试有认证信息时返回用户名
     */
    @Test
    void getCurrentUserReturnsNameWhenAuthenticated() {
        // given: 设置认证
        String expectedUsername = "test-user";
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(expectedUsername, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        String currentUser = SecurityUtils.getCurrentUser();

        // then
        Assertions.assertEquals(expectedUsername, currentUser);
    }

    /**
     * 测试未认证就算有对象也返回null
     */
    @Test
    void getCurrentUserReturnsNullWhenNotAuthenticatedFlag() {
        // given: 设置认证但是未认证
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("test-user", null);
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        String currentUser = SecurityUtils.getCurrentUser();

        // then
        Assertions.assertNull(currentUser);
    }

    /**
     * 测试获取角色，去掉ROLE_前缀
     */
    @Test
    void getRolesStripsRolePrefix() {
        // given: 用户有多个角色，其中一个带ROLE_前缀
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority(SecurityConstants.ROOT_ROLE_CODE)
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        Set<String> roles = SecurityUtils.getRoles();

        // then
        Assertions.assertEquals(3, roles.size());
        Assertions.assertTrue(roles.contains("ADMIN"));
        Assertions.assertTrue(roles.contains("USER"));
        Assertions.assertTrue(roles.contains(SecurityConstants.ROOT_ROLE_CODE));
    }

    /**
     * 测试没有权限时返回空集合
     */
    @Test
    void getRolesReturnsEmptySetWhenNoAuthorities() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        Set<String> roles = SecurityUtils.getRoles();

        // then
        Assertions.assertTrue(roles.isEmpty());
    }

    /**
     * 测试判断是否超级管理员
     */
    @Test
    void isRootReturnsTrueWhenUserHasRootRole() {
        // given
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(SecurityConstants.ROOT_ROLE_CODE)
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        boolean isRoot = SecurityUtils.isRoot();

        // then
        Assertions.assertTrue(isRoot);
    }

    /**
     * 测试从JWT获取token attributes
     */
    @Test
    void getTokenAttributesReturnsAttributesFromJwt() {
        // given: JWT token包含自定义claims
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.JWT_NICKNAME_KEY, "张三");
        claims.put(SecurityConstants.JWT_DEPTID_KEY, 100L);
        claims.put(SecurityConstants.JWT_DATASCOPE_KEY, 1);
        claims.put(GlobalConstants.TENANT_ID_NAME, 1001L);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuedAt(Instant.now())
                .claims(c -> c.putAll(claims))
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(token);

        // when: 获取各个属性
        String nickname = SecurityUtils.getNickname();
        Long deptId = SecurityUtils.getDeptId();
        Integer dataScope = SecurityUtils.getDataScope();
        Long tenantId = SecurityUtils.getTenantId();
        Map<String, Object> attributes = SecurityUtils.getTokenAttributes();

        // then: 所有属性正确获取。Jwt会自动添加exp和iat，所以总共有4 + 2 = 6个属性
        Assertions.assertEquals("张三", nickname);
        Assertions.assertEquals(100L, deptId);
        Assertions.assertEquals(1, dataScope);
        Assertions.assertEquals(1001L, tenantId);
        Assertions.assertEquals(claims.size() + 2, attributes.size());
    }

    /**
     * 测试获取token id从JwtAuthenticationToken
     */
    @Test
    void getTokenIdReturnsIdFromJwt() {
        // given
        Jwt jwt = Jwt.withTokenValue("test-token-value")
                .header("alg", "RS256")
                .claim("jti", "test-token-id")
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuedAt(Instant.now())
                .build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(token);

        // when
        String tokenId = SecurityUtils.getTokenId();

        // then: jwt id 从jti claim获取，这里我们只要验证能获取到
        Assertions.assertNotNull(tokenId);
        Assertions.assertEquals("test-token-id", tokenId);
    }

    /**
     * 测试不是JwtAuthenticationToken时返回null
     */
    @Test
    void getTokenIdReturnsNullWhenNotJwt() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        String tokenId = SecurityUtils.getTokenId();

        // then
        Assertions.assertNull(tokenId);
    }

    /**
     * 没有认证信息时返回空map
     */
    @Test
    void getTokenAttributesReturnsEmptyMapWhenNoAuthentication() {
        // given: SecurityContext是空的

        // when
        Map<String, Object> attributes = SecurityUtils.getTokenAttributes();

        // then
        Assertions.assertTrue(attributes.isEmpty());
    }
}
