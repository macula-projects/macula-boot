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

package dev.macula.boot.starter.system.test;

import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.system.dto.RouteVO;
import dev.macula.boot.starter.system.dto.UserLoginVO;
import dev.macula.boot.starter.system.service.PermissionService;
import dev.macula.boot.starter.system.service.SystemService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * <p>
 * <b>PermissionServiceTest</b> 权限校验服务单元测试
 * </p>
 * <p>
 * 测试 hasPermission 方法对不同用户角色权限匹配是否正确
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class PermissionServiceTest {

    private TestSystemService testSystemService;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testSystemService = new TestSystemService();
        permissionService = new PermissionService(testSystemService);
    }

    /**
     * 测试空权限字符串返回false
     */
    @Test
    void hasPermissionReturnsFalseWhenBlankPermission() {
        // given
        String emptyPerm = "";
        String nullPerm = null;
        String blankPerm = "   ";

        // when
        boolean resultEmpty = permissionService.hasPermission(emptyPerm);
        boolean resultNull = permissionService.hasPermission(nullPerm);
        boolean resultBlank = permissionService.hasPermission(blankPerm);

        // then
        Assertions.assertFalse(resultEmpty);
        Assertions.assertFalse(resultNull);
        Assertions.assertFalse(resultBlank);
    }

    /**
     * 测试超级管理员直接返回true
     */
    @Test
    void hasPermissionReturnsTrueForRootUser() {
        // given: 用户是超级管理员
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority(SecurityConstants.ROOT_ROLE_CODE)
        );
        // When authorities are provided in constructor, token is already authenticated
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("root", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        boolean result = permissionService.hasPermission("any:permission");

        // then: 超级管理员任何权限都返回true
        Assertions.assertTrue(result);
    }

    /**
     * 测试用户有确切权限匹配返回true
     */
    @Test
    void hasPermissionReturnsTrueWhenExactMatch() {
        // given: 普通用户，权限包含system:user:view
        givenUserWithPermissions(Set.of("system:user:view", "system:user:add"));

        // when
        boolean result = permissionService.hasPermission("system:user:view");

        // then
        Assertions.assertTrue(result);
    }

    /**
     * 测试Ant风格通配符匹配
     */
    @Test
    void hasPermissionMatchesWildcardPattern() {
        // given: 用户有system:* 权限，可以匹配system任何子权限
        givenUserWithPermissions(Set.of("system:*"));

        // when: 不同的通配符情况
        boolean matchSystemUser = permissionService.hasPermission("system:user:view");
        boolean matchDept = permissionService.hasPermission("system:dept:list");
        boolean noMatchOther = permissionService.hasPermission("other:test");

        // then
        Assertions.assertTrue(matchSystemUser);
        Assertions.assertTrue(matchDept);
        Assertions.assertFalse(noMatchOther);
    }

    /**
     * 测试多级通配符匹配
     */
    @Test
    void hasPermissionMatchesMultiLevelWildcard() {
        // given: 用户system:user:* 权限
        givenUserWithPermissions(Set.of("system:user:*"));

        // when
        boolean matchView = permissionService.hasPermission("system:user:view");
        boolean matchAdd = permissionService.hasPermission("system:user:add");
        boolean noMatchOther = permissionService.hasPermission("system:dept:list");

        // then
        Assertions.assertTrue(matchView);
        Assertions.assertTrue(matchAdd);
        Assertions.assertFalse(noMatchOther);
    }

    /**
     * 测试用户没有权限返回false
     */
    @Test
    void hasPermissionReturnsFalseWhenNoMatchingPermission() {
        // given:
        givenUserWithPermissions(Set.of("system:user:view"));

        // when
        boolean result = permissionService.hasPermission("system:user:add");

        // then
        Assertions.assertFalse(result);
    }

    /**
     * 测试用户权限集合为空返回false
     */
    @Test
    void hasPermissionReturnsFalseWhenEmptyPermissions() {
        // given:
        givenUserWithPermissions(Collections.emptySet());

        // when
        boolean result = permissionService.hasPermission("system:user:view");

        // then
        Assertions.assertFalse(result);
    }

    /**
     * 测试获取用户信息失败返回false
     */
    @Test
    void hasPermissionReturnsFalseWhenSystemServiceThrows() {
        // given: systemService抛出异常
        givenUserWithPermissions(null);

        // when
        boolean result = permissionService.hasPermission("system:user:view");

        // then
        Assertions.assertFalse(result);
    }

    /**
     * 设置测试用户和权限
     */
    private void givenUserWithPermissions(Set<String> permissions) {
        if (permissions != null) {
            UserLoginVO user = new UserLoginVO();
            user.setPerms(permissions);
            testSystemService.setUserSupplier(() -> user);
        } else {
            testSystemService.setUserSupplier(() -> {
                throw new RuntimeException("simulate failure");
            });
        }

        // When authorities are provided in constructor, token is already authenticated
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * 测试用的SystemService实现，可定制返回值
     */
    static class TestSystemService implements SystemService {
        private Supplier<UserLoginVO> userSupplier = () -> null;

        public void setUserSupplier(Supplier<UserLoginVO> userSupplier) {
            this.userSupplier = userSupplier;
        }

        @Override
        public UserLoginVO getUseInfo() {
            return userSupplier.get();
        }

        @Override
        public List<RouteVO> listRoutes() {
            return Collections.emptyList();
        }
    }
}
