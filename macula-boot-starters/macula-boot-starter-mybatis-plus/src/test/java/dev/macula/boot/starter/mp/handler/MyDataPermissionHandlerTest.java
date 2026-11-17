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


package dev.macula.boot.starter.mp.handler;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import dev.macula.boot.starter.mp.interceptor.DataPermissionParameterContext;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证数据权限条件不受用户名或 OR 分支绕过。
 *
 * @author Rain
 * @since 6.1.0
 */
class MyDataPermissionHandlerTest {
    /** 清理认证，避免不同测试之间串用身份。 */
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 构造带可信属性的已认证内省身份。 */
    private void authenticate(String name, Integer scope) {
        var attributes = new HashMap<String, Object>();
        attributes.put("deptId", 12L);
        if (scope != null) {
            attributes.put("dataScope", scope);
        }
        var principal = new DefaultOAuth2AuthenticatedPrincipal(name, attributes, List.of());
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.EPOCH, Instant.MAX);
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, token, List.of()));
    }

    /** 原始用户名仅作为额外参数，最终 SQL 不包含值或方言函数。 */
    @Test
    void bindsUsernameAsData() {
        for (String name : new String[] {"alice\" OR 1=1 --", "O'Reilly", "中文\\名字", "😀", "alice"}) {
            authenticate(name, 3);
            try (var context = DataPermissionParameterContext.open()) {
                var expression = MyDataPermissionHandler.dataScopeFilter("", "dept_id", "u", "create_by", null);
                Configuration configuration = new Configuration();
                BoundSql bound = new BoundSql(configuration, "SELECT * FROM t WHERE " + expression, List.of(), null);
                context.apply(configuration, bound);
                assertThat(bound.getSql()).isEqualTo("SELECT * FROM t WHERE u.create_by = ?");
                assertThat(bound.getParameterMappings()).hasSize(1);
                assertThat(bound.getAdditionalParameter(bound.getParameterMappings().get(0).getProperty())).isEqualTo(name);
            }
        }
    }

    /** 原 WHERE 中的 OR 必须整体受绑定的部门参数约束。 */
    @Test
    void groupsOriginalOrCondition() throws Exception {
        authenticate("alice", 2);
        try (var context = DataPermissionParameterContext.open()) {
            var where = CCJSqlParserUtil.parseCondExpression("active = 1 OR id = 2");
            var expression = MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", where);
            var configuration = new Configuration();
            var bound = new BoundSql(configuration, "SELECT * FROM t WHERE " + expression, List.of(), null);
            context.apply(configuration, bound);
            assertThat(bound.getSql()).isEqualTo("SELECT * FROM t WHERE (active = 1 OR id = 2) AND (dept_id = ?)");
            assertThat(bound.getAdditionalParameter(bound.getParameterMappings().get(0).getProperty())).isEqualTo(12L);
        }
    }

    /** 缺失和 DEFAULT 继续放行，非法范围不转为 ALL。 */
    @Test
    void preservesScopePolicy() throws Exception {
        var where = CCJSqlParserUtil.parseCondExpression("active = 1");
        for (Integer scope : new Integer[] {null, 0, 9}) {
            authenticate("alice", scope);
            assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", where)).isSameAs(where);
        }
        authenticate("alice", 12345);
        assertThatThrownBy(() -> MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", where))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /** 空用户名和恶意标识符不能产生不受控条件。 */
    @Test
    void rejectsUnsafeIdentifiersAndDeniesMissingUser() {
        authenticate("alice", 3);
        assertThatThrownBy(() -> MyDataPermissionHandler.dataScopeFilter("", "dept_id", "u OR 1=1", "create_by", null))
            .isInstanceOf(IllegalArgumentException.class);
        SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);
        assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", null).toString()).isEqualTo("1 = 0");
    }
    /** 部门范围保留参数化 SQL 子查询，ROOT 保留豁免。 */
    @Test
    void keepsDepartmentSubqueryAndRootExemption() throws Exception {
        authenticate("alice", 1);
        try (var context = DataPermissionParameterContext.open()) {
            var where = CCJSqlParserUtil.parseCondExpression("active = 1 OR (id = 2 AND active = 0)");
            var expression = MyDataPermissionHandler.dataScopeFilter("d", "dept_id", "", "create_by", where);
            var configuration = new Configuration();
            var bound = new BoundSql(configuration, "SELECT * FROM t WHERE " + expression, List.of(), null);
            context.apply(configuration, bound);
            assertThat(bound.getSql()).startsWith("SELECT * FROM t WHERE (active = 1 OR (id = 2 AND active = 0)) AND (")
                .contains("d.dept_id IN (SELECT id FROM sys_dept", "tree_path LIKE ?").doesNotContain("find_in_set", "CONVERT");
        }
        var where = CCJSqlParserUtil.parseCondExpression("active = 1");
        var authentication = (BearerTokenAuthentication)SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication((OAuth2AuthenticatedPrincipal)authentication.getPrincipal(),
            authentication.getToken(), AuthorityUtils.createAuthorityList("ROLE_ROOT")));
        assertThat(new MyDataPermissionHandler().getSqlSegment(where, "not.a.real.Mapper.method")).isSameAs(where);
    }

    /** 缺少参数绑定环境时不能生成未绑定的权限条件。 */
    @Test
    void requiresBindingContext() {
        authenticate("alice", 3);
        assertThatThrownBy(() -> MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", null))
            .isInstanceOf(IllegalStateException.class);
    }
}
