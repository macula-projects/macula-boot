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


package dev.macula.boot.starter.mp.test;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.macula.boot.starter.mp.annotation.DataPermission;
import dev.macula.boot.starter.mp.interceptor.ParameterizedDataPermissionInterceptor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在 H2 默认及多种兼容模式执行真实 MyBatis 参数绑定、查询与写操作。
 * 兼容模式验证 SQL 的可移植形态，不等同于真实厂商数据库认证。
 *
 * @author Rain
 * @since 6.1.0
 */
class DataPermissionDatabaseIT {
    /** 清理认证身份。 */
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 构造来自内省结果的可信身份。 */
    private void authenticate(String username, int scope) {
        var principal = new DefaultOAuth2AuthenticatedPrincipal(username, Map.of("dataScope", scope, "deptId", 12L), List.of());
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.EPOCH, Instant.MAX);
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, token, List.of()));
    }

    /** 五种模式中执行同一套权限用例，包含原参数与权限参数交织的 UNION。 */
    @Test
    void executesPortableQueriesAndWrites() throws Exception {
        for (String mode : new String[] {"", "MySQL", "PostgreSQL", "Oracle", "MSSQLServer"}) {
            var source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:permission_" + UUID.randomUUID().toString().replace("-", "")
                + (mode.isEmpty() ? "" : ";MODE=" + mode));
            try (var fixture = source.getConnection(); var statement = fixture.createStatement()) {
                statement.execute("CREATE TABLE sys_dept (id BIGINT PRIMARY KEY, tree_path VARCHAR(100))");
                statement.execute("INSERT INTO sys_dept VALUES (12, '0'), (13, '0,12'), (22, '112'),"
                    + " (31, '12'), (32, '12,999'), (41, '0,12,999'), (51, '0,112'), (61, '0,12'), (70, NULL)");
                statement.execute("CREATE TABLE secure_records (id INT PRIMARY KEY, active INT, dept_id BIGINT, create_by VARCHAR(255))");
                String[] names = {"alice", "O'Reilly", "alice\" OR 1=1 --", "中文\\用户", "😀", "other", "last", "tail"};
                long[] departments = {12, 13, 22, 31, 32, 41, 51, 61};
                try (var insert = fixture.prepareStatement("INSERT INTO secure_records VALUES (?, 1, ?, ?)")) {
                    for (int i = 0; i < names.length; i++) {
                        insert.setInt(1, i + 1);
                        insert.setLong(2, departments[i]);
                        insert.setString(3, names[i]);
                        insert.executeUpdate();
                    }
                }
                Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), source));
                var plugins = new MybatisPlusInterceptor();
                plugins.addInnerInterceptor(new ParameterizedDataPermissionInterceptor());
                if (mode.isEmpty()) {
                    plugins.addInnerInterceptor(new PaginationInnerInterceptor());
                }
                configuration.addInterceptor(plugins);
                configuration.addMapper(Mapper.class);
                var factory = new SqlSessionFactoryBuilder().build(configuration);
                try (var session = factory.openSession(true)) {
                    Mapper mapper = session.getMapper(Mapper.class);
                    for (int i = 0; i < names.length; i++) {
                        authenticate(names[i], 3);
                        assertThat(mapper.list(1)).as(mode + ": SELF").containsExactly(i + 1);
                    }
                    authenticate("alice", 3);
                    if (mode.equals("MSSQLServer")) {
                        assertThat(mapper.top(2, 1)).as("TOP binds limit before permission").containsExactly(1);
                    }
                    assertThat(mapper.union(1, 2)).as(mode + ": UNION").containsExactly(1);
                    assertThat(mapper.nested(1)).as(mode + ": subquery").containsExactly(1);
                    assertThat(mapper.foreach(List.of(1, 2))).as(mode + ": foreach").containsExactly(1);
                    authenticate("alice", 1);
                    assertThat(mapper.list(1)).as(mode + ": department delimiter").containsExactly(1, 2, 4, 5, 6, 8);
                    if (mode.isEmpty()) {
                        Page<Integer> page = new Page<>(1, 2);
                        assertThat(mapper.paged(page, 1)).containsExactly(1, 2);
                        assertThat(page.getTotal()).isEqualTo(6);
                    }
                    authenticate("alice", 2);
                    assertThat(mapper.list(1)).as(mode + ": department equality").containsExactly(1);
                    authenticate("alice", 3);
                    assertThat(mapper.update(2, 2)).as(mode + ": deny foreign update").isZero();
                    assertThat(mapper.update(1, 2)).as(mode + ": allow own update").isEqualTo(1);
                    assertThat(mapper.list(2)).containsExactly(1);
                    assertThat(mapper.delete(2)).as(mode + ": deny foreign delete").isZero();
                    assertThat(mapper.delete(1)).as(mode + ": allow own delete").isEqualTo(1);
                    assertThat(mapper.list(2)).isEmpty();
                }
            }
        }
    }

    /**
     * 验证实际 SQL 执行的 Mapper；参数由 MyBatis 类型处理器写入 PreparedStatement。
     * @since 6.1.0
     */
    interface Mapper {
        /** SQL Server TOP 原参数与新增权限参数必须按实际位置绑定。 */
        @DataPermission
        @Select("SELECT TOP (#{limit}) id FROM secure_records WHERE active = #{active} ORDER BY id")
        List<Integer> top(@Param("limit") int limit, @Param("active") int active);

        /** 按原状态条件查询，再由插件追加权限。 */
        @DataPermission
        @Select("SELECT id FROM secure_records WHERE active = #{active} ORDER BY id")
        List<Integer> list(@Param("active") int active);

        /** 各 UNION 分支原参数与权限参数必须正确交织。 */
        @DataPermission
        @Select("SELECT id FROM secure_records WHERE id = #{left} UNION ALL SELECT id FROM secure_records WHERE id = #{right}")
        List<Integer> union(@Param("left") int left, @Param("right") int right);

        /** 子查询原参数不能受外层权限绑定影响。 */
        @DataPermission
        @Select("SELECT id FROM secure_records WHERE id IN (SELECT id FROM secure_records WHERE active = #{active}) ORDER BY id")
        List<Integer> nested(@Param("active") int active);

        /** 动态 foreach 的 additionalParameters 必须保留。 */
        @DataPermission
        @Select({"<script>SELECT id FROM secure_records WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "ORDER BY id</script>"})
        List<Integer> foreach(@Param("ids") List<Integer> ids);

        /** 分页 count 与分页查询都应复用已绑定的权限参数。 */
        @DataPermission
        @Select("SELECT id FROM secure_records WHERE active = #{active} ORDER BY id")
        List<Integer> paged(Page<Integer> page, @Param("active") int active);

        /** 写操作 SET、原 WHERE 和追加权限参数均需保留正确位置。 */
        @DataPermission
        @Update("UPDATE secure_records SET active = #{active} WHERE id = #{id}")
        int update(@Param("id") int id, @Param("active") int active);

        /** 删除操作同样受到参数化权限条件约束。 */
        @DataPermission
        @Delete("DELETE FROM secure_records WHERE id = #{id}")
        int delete(@Param("id") int id);
    }
}
