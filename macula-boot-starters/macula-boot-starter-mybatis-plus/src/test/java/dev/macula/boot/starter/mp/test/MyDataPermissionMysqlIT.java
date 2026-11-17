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

import dev.macula.boot.starter.mp.handler.MyDataPermissionHandler;
import dev.macula.boot.starter.mp.interceptor.DataPermissionParameterContext;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import net.sf.jsqlparser.expression.Expression;
import java.sql.PreparedStatement;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在显式指定的独立测试 MySQL 中验证 SQL mode、用户名比较和实际权限结果。
 * 不读取业务数据源，仅创建连接级临时表，连接关闭后自动销毁。
 *
 * @author Rain
 * @since 6.1.0
 */
@EnabledIfEnvironmentVariable(named = "MACULA_SECURITY_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
class MyDataPermissionMysqlIT {
    /** 清理测试身份。 */
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 使用真实内省身份驱动原有数据权限入口。 */
    private void authenticate(String name, int scope) {
        var principal = new DefaultOAuth2AuthenticatedPrincipal(name, Map.of("dataScope", scope, "deptId", 12L), List.of());
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.EPOCH, Instant.MAX);
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, token, List.of()));
    }

    /** 只在专用环境下创建随机临时表，验证有真实数据的 SQL 结果。 */
    @Test
    void enforcesPermissionAcrossSqlModes() throws Exception {
        try (Connection connection = DriverManager.getConnection(System.getenv("MACULA_SECURITY_TEST_MYSQL_URL"),
            System.getenv("MACULA_SECURITY_TEST_MYSQL_USER"), System.getenv("MACULA_SECURITY_TEST_MYSQL_PASSWORD"))) {
            String table = "security_probe_" + UUID.randomUUID().toString().replace("-", "");
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TEMPORARY TABLE " + table
                    + " (id INT, active INT, dept_id BIGINT, create_by VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci)");
                try {
                    for (String mode : new String[] {"", "ANSI_QUOTES", "NO_BACKSLASH_ESCAPES"}) {
                        statement.execute("SET SESSION sql_mode = '" + mode + "'");
                        statement.executeUpdate("DELETE FROM " + table);
                        String[] names = {"alice", "O'Reilly", "alice\" OR 1=1 --", "中文\\用户", "😀"};
                        try (var insert = connection.prepareStatement("INSERT INTO " + table + " VALUES (?, 1, 99, ?)")) {
                            for (int i = 0; i < names.length; i++) {
                                insert.setInt(1, i + 1);
                                insert.setString(2, names[i]);
                                insert.executeUpdate();
                            }
                        }
                        for (int i = 0; i < names.length; i++) {
                            authenticate(names[i], 3);
                            try (var context = DataPermissionParameterContext.open();
                                var query = prepare(connection, table,
                                    MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by", null), context);
                                var result = query.executeQuery()) {
                                assertThat(result.next()).isTrue();
                                assertThat(result.getInt(1)).isEqualTo(i + 1);
                                assertThat(result.next()).isFalse();
                            }
                        }
                        authenticate("alice", 2);
                        try (var context = DataPermissionParameterContext.open();
                            var query = prepare(connection, table, MyDataPermissionHandler.dataScopeFilter("", "dept_id", "", "create_by",
                                CCJSqlParserUtil.parseCondExpression("active = 1 OR id = 2")), context);
                            var result = query.executeQuery()) {
                            assertThat(result.next()).isFalse();
                        }
                    }
                } finally {
                    statement.execute("DROP TEMPORARY TABLE " + table);
                }
            }
        }
    }
    /** 通过 MyBatis 类型处理器绑定权限值，不拼接测试用户名。 */
    private PreparedStatement prepare(Connection connection, String table, Expression condition,
        DataPermissionParameterContext context) throws Exception {
        var configuration = new Configuration();
        var bound = new BoundSql(configuration, "SELECT id FROM " + table + " WHERE " + condition, List.of(), null);
        context.apply(configuration, bound);
        var mapped = new MappedStatement.Builder(configuration, "probe", new StaticSqlSource(configuration, bound.getSql()),
            SqlCommandType.SELECT).build();
        PreparedStatement statement = connection.prepareStatement(bound.getSql());
        try {
            new DefaultParameterHandler(mapped, null, bound).setParameters(statement);
            return statement;
        } catch (Exception failure) {
            statement.close();
            throw failure;
        }
    }
}
