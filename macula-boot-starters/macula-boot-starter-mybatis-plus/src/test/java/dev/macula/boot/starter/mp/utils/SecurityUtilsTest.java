/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 ROOT 只能来自已认证的非匿名身份。
 *
 * @author Rain
 * @since 6.1.0
 */
class SecurityUtilsTest {
    /** 清除线程身份。 */
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 未认证或匿名身份不能凭角色名获得 ROOT。 */
    @Test
    void requiresAuthenticatedRoot() {
        var authorities = AuthorityUtils.createAuthorityList("ROLE_ROOT");
        assertThat(SecurityUtils.isRoot()).isFalse();
        var root = new UsernamePasswordAuthenticationToken("root", "", authorities);
        SecurityContextHolder.getContext().setAuthentication(root);
        assertThat(SecurityUtils.isRoot()).isTrue();
        root.setAuthenticated(false);
        assertThat(SecurityUtils.isRoot()).isFalse();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymous", authorities));
        assertThat(SecurityUtils.isRoot()).isFalse();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "", AuthorityUtils.NO_AUTHORITIES));
        assertThat(SecurityUtils.isRoot()).isFalse();
    }
}
