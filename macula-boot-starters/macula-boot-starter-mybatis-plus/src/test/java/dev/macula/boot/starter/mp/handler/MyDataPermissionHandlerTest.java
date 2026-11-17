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

package dev.macula.boot.starter.mp.handler;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证原数据权限结构内的用户名白名单、SQL 分组及既有范围语义。
 * @author Rain
 * @since 6.1.0
 */
class MyDataPermissionHandlerTest {
    /** 使用认证上下文提供姓名，包含真实 principal 通常拒绝的空值边界。 */
    private void authenticate(String username, Integer scope) {
        Map<String, Object> attributes = new HashMap<>();
        if (scope != null) {
            attributes.put("dataScope", scope);
        }
        attributes.put("deptId", 12L);
        BearerTokenAuthentication authentication = mock(BearerTokenAuthentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getTokenAttributes()).thenReturn(attributes);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 防止身份污染后续测试。 */
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 只接受约定的 ASCII 用户名，保持原大小写及下划线，不转换身份。 */
    @Test
    void acceptsDeclaredUsernameAlphabet() {
        for (String name : Arrays.asList("alice", "ALICE", "aZ_019", "_", "123", "SELECT", "NULL")) {
            authenticate(name, 3);
            assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", null).toString())
                .isEqualTo("create_by = '" + name + "'");
        }
    }

    /** 非法/空身份返回无匹配条件，不删除字符后误匹配其他合法用户。 */
    @Test
    void deniesInvalidNamesWithoutSanitizingThem() {
        for (String name : Arrays.asList(null, "", " ", "alice ", " alice", "alice-admin", "alice.admin",
            "alice@example.com", "用户", "Ａlice", "alice\\admin", "alice\nadmin", "alice\tadmin",
            "alice\" OR 1=1 --", "alice' OR 1=1 -- ")) {
            authenticate(name, 3);
            assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", null).toString())
                .isEqualTo("1 = 0");
        }
    }

    /** 原 OR 条件与有效或无效身份的权限条件都必须独立分组。 */
    @Test
    void groupsOriginalOrAndPermission() throws Exception {
        Expression where = CCJSqlParserUtil.parseCondExpression("active = 1 OR id = 2");
        authenticate("alice", 3);
        assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", where).toString())
            .isEqualTo("(active = 1 OR id = 2) AND (create_by = 'alice')");
        authenticate("alice' OR 1=1 --", 3);
        assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", where).toString())
            .isEqualTo("(active = 1 OR id = 2) AND (1 = 0)");
    }

    /** ALL、DEFAULT 和缺失范围继续返回原条件，不趁机改变现有授权策略。 */
    @Test
    void preservesUnrestrictedScopes() throws Exception {
        Expression where = CCJSqlParserUtil.parseCondExpression("active = 1");
        for (Integer scope : Arrays.asList(0, 9, null)) {
            authenticate("invalid-name", scope);
            assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", where)).isSameAs(where);
        }
    }

    /** 部门 ID 和 MySQL 子部门函数保持原查询形态，仅原 OR 条件补分组。 */
    @Test
    void preservesDepartmentQueries() throws Exception {
        Expression where = CCJSqlParserUtil.parseCondExpression("active = 1 OR id = 2");
        authenticate("alice", 2);
        assertThat(MyDataPermissionHandler.dataScopeFilter("d", "dept_id", "", "create_by", where).toString())
            .isEqualTo("(active = 1 OR id = 2) AND (d.dept_id = 12)");
        authenticate("alice", 1);
        assertThat(MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", null).toString())
            .contains("id = 12", "find_in_set(12, tree_path)");
    }
}
