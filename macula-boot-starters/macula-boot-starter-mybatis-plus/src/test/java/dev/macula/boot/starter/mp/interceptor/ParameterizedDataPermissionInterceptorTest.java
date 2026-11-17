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


package dev.macula.boot.starter.mp.interceptor;

import dev.macula.boot.starter.mp.annotation.DataPermission;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证参数实际输出顺序、嵌套与线程隔离，不以字符串替换猜测问号位置。
 *
 * @author Rain
 * @since 6.1.0
 */
class ParameterizedDataPermissionInterceptorTest {
    private final Configuration configuration = new Configuration();

    /** 每个测试使用普通已认证身份。 */
    @BeforeEach
    void authenticate() {
        authenticate(3);
    }

    /** 设置固定身份与范围。 */
    private void authenticate(int scope) {
        var principal = new DefaultOAuth2AuthenticatedPrincipal("alice' OR 1=1 --", Map.of("dataScope", scope, "deptId", 12L), List.of());
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.EPOCH, Instant.MAX);
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, token, List.of()));
    }

    /** 防止后续测试继承认证状态。 */
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 创建有原参数的 MappedStatement 并执行查询改写。 */
    private BoundSql rewrite(String sql, List<ParameterMapping> mappings, Object parameters) throws Exception {
        MappedStatement statement = new MappedStatement.Builder(configuration, Mapper.class.getName() + ".select",
            new StaticSqlSource(configuration, sql, mappings), SqlCommandType.SELECT).build();
        BoundSql bound = statement.getBoundSql(parameters);
        new ParameterizedDataPermissionInterceptor().beforeQuery(null, statement, parameters, RowBounds.DEFAULT, null, bound);
        return bound;
    }

    /** 读取 MyBatis 将传给 JDBC 的参数值，包含原对象与新增参数。 */
    private List<Object> values(BoundSql bound) {
        List<Object> values = new ArrayList<>();
        for (var mapping : bound.getParameterMappings()) {
            String property = mapping.getProperty();
            values.add(bound.hasAdditionalParameter(property) ? bound.getAdditionalParameter(property)
                : configuration.newMetaObject(bound.getParameterObject()).getValue(property));
        }
        return values;
    }

    /** 原参数位于 SELECT、WHERE 和 LIMIT 时，权限参数必须插入其实际位置。 */
    @Test
    void interleavesBeforeLimit() throws Exception {
        var mappings = List.of(new ParameterMapping.Builder(configuration, "label", String.class).build(),
            new ParameterMapping.Builder(configuration, "active", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "sort", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "limit", Integer.class).build());
        BoundSql bound = rewrite("SELECT ? AS label, '?' AS literal FROM t WHERE active = ? ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END LIMIT ?", mappings,
            Map.of("label", "test", "active", 1, "sort", 7, "limit", 10));
        assertThat(values(bound)).containsExactly("test", 1, "alice' OR 1=1 --", 7, 10);
        assertThat(bound.getSql()).contains("'?'", "create_by = ?", "LIMIT ?").doesNotContain("alice", "CONVERT");
        assertThat(bound.getParameterMappings().get(2).getJavaType()).isEqualTo(String.class);
    }

    /** TOP 表达式中的参数必须先于 WHERE 与权限参数消费，选项保持不变。 */
    @Test
    void preservesTopParametersAndOptions() throws Exception {
        var mappings = List.of(new ParameterMapping.Builder(configuration, "limit", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "active", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "sort", Integer.class).build());
        for (String top : List.of("TOP (?)", "TOP ?", "TOP (?) PERCENT", "TOP (?) WITH TIES")) {
            var bound = rewrite("SELECT " + top + " id FROM t WHERE active = ? ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END",
                mappings, Map.of("limit", 10, "active", 1, "sort", 7));
            assertThat(values(bound)).containsExactly(10, 1, "alice' OR 1=1 --", 7);
            assertThat(bound.getSql()).contains(top);
        }
    }

    /** 层级子句两种顺序和 NOCYCLE 均不能跳过内部的原参数。 */
    @Test
    void preservesHierarchicalParametersAndClauseOrder() throws Exception {
        var mappings = List.of(new ParameterMapping.Builder(configuration, "active", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "first", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "second", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "sort", Integer.class).build());
        for (String hierarchy : List.of(
            "START WITH parent_id = ? CONNECT BY NOCYCLE PRIOR id = parent_id AND depth < ?",
            "CONNECT BY NOCYCLE PRIOR id = parent_id AND depth < ? START WITH parent_id = ?",
            "START WITH parent_id = ? CONNECT BY id = PRIOR parent_id AND depth < ?")) {
            var bound = rewrite("SELECT id FROM t WHERE active = ? " + hierarchy
                + " ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END", mappings,
                Map.of("active", 1, "first", 12, "second", 4, "sort", 7));
            assertThat(values(bound)).containsExactly(1, "alice' OR 1=1 --", 12, 4, 7);
            assertThat(bound.getSql()).contains(hierarchy);
        }
    }

    /** UNION 各分支的权限参数分别插入，不能统一追加到最后。 */
    @Test
    void interleavesUnionBranches() throws Exception {
        var mappings = List.of(new ParameterMapping.Builder(configuration, "left", Integer.class).build(),
            new ParameterMapping.Builder(configuration, "right", Integer.class).build());
        var bound = rewrite("SELECT id FROM t WHERE id = ? UNION ALL SELECT id FROM t WHERE id = ?", mappings,
            Map.of("left", 1, "right", 2));
        assertThat(values(bound)).containsExactly(1, "alice' OR 1=1 --", 2, "alice' OR 1=1 --");
    }

    /** 子查询中的原参数不能被最外层新增权限参数覆盖。 */
    @Test
    void preservesSubqueryParameters() throws Exception {
        var mappings = List.of(new ParameterMapping.Builder(configuration, "score", Integer.class).build());
        var bound = rewrite("SELECT id FROM t WHERE id IN (SELECT id FROM s WHERE score = ?)", mappings,
            Map.of("score", 7));
        assertThat(values(bound)).containsExactly(7, "alice' OR 1=1 --");
    }

    /** 部门范围保留 SQL 子查询，仅绑定固定数量的部门及路径参数。 */
    @Test
    void keepsDepartmentRangeInSqlSubquery() throws Exception {
        authenticate(1);
        var bound = rewrite("SELECT id FROM t", List.of(), null);
        assertThat(bound.getSql()).contains("IN (SELECT id FROM sys_dept", "tree_path LIKE ?").doesNotContain("find_in_set");
        assertThat(values(bound)).containsExactly(12L, "12", "12,%", "%,12,%", "%,12");
    }

    /** 嵌套查询完成或失败后恢复外层上下文，已有 foreach 参数不会丢失。 */
    @Test
    void restoresNestedContextAndExistingAdditionalParameters() {
        try (var outer = DataPermissionParameterContext.open()) {
            var first = DataPermissionParameterContext.bind("outer");
            try (var inner = DataPermissionParameterContext.open()) {
                var parameter = DataPermissionParameterContext.bind("inner");
                var innerSql = new BoundSql(configuration, "SELECT " + parameter, List.of(), null);
                inner.apply(configuration, innerSql);
                assertThat(values(innerSql)).containsExactly("inner");
            }
            var second = DataPermissionParameterContext.bind("outer2");
            var original = new ParameterMapping.Builder(configuration, "__frch_item_0", String.class).build();
            var bound = new BoundSql(configuration, "SELECT ?, " + first + ", " + second, List.of(original), null);
            bound.setAdditionalParameter("__frch_item_0", "foreach");
            outer.apply(configuration, bound);
            assertThat(values(bound)).containsExactly("foreach", "outer", "outer2");
        }
        assertThatThrownBy(() -> DataPermissionParameterContext.bind("leaked")).isInstanceOf(IllegalStateException.class);
    }

    /** 参数映射不完整时拒绝执行，并清理上下文。 */
    @Test
    void rejectsMismatchWithoutLeakingContext() {
        assertThatThrownBy(() -> rewrite("SELECT id FROM t WHERE id = ?", List.of(), null))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> DataPermissionParameterContext.bind("leaked")).isInstanceOf(IllegalStateException.class);
    }

    /** 自定义 SqlSource 可以省略空映射集合，新增权限值仍可绑定。 */
    @Test
    void acceptsMissingOriginalMappings() {
        try (var context = DataPermissionParameterContext.open()) {
            var parameter = DataPermissionParameterContext.bind("value");
            var bound = new BoundSql(configuration, "SELECT " + parameter, null, null);
            context.apply(configuration, bound);
            assertThat(values(bound)).containsExactly("value");
        }
    }

    /** 不同线程的参数集合相互独立。 */
    @Test
    void isolatesConcurrentContexts() {
        var futures = List.of("first", "second").stream().map(value -> CompletableFuture.supplyAsync(() -> {
            try (var context = DataPermissionParameterContext.open()) {
                var parameter = DataPermissionParameterContext.bind(value);
                var bound = new BoundSql(configuration, "SELECT " + parameter, List.of(), null);
                context.apply(configuration, bound);
                return bound.getAdditionalParameter(bound.getParameterMappings().get(0).getProperty());
            }
        })).toList();
        assertThat(futures.stream().map(CompletableFuture::join).toList()).containsExactly("first", "second");
    }

    /**
     * 为拦截器提供与真实 Mapper 相同的注解定位入口。
     * @since 6.1.0
     */
    interface Mapper {
        /** 使用数据权限的查询标记。 */
        @DataPermission
        void select();
    }
}
