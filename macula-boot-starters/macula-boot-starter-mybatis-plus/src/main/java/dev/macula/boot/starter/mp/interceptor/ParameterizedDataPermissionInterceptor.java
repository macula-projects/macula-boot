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

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import dev.macula.boot.starter.mp.handler.MyDataPermissionHandler;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 将数据权限 AST 中的值转换为真实 JDBC 参数，兼容查询和更新/删除的参数映射。
 *
 * @author Rain
 * @since 6.1.0
 */
public class ParameterizedDataPermissionInterceptor extends DataPermissionInterceptor {
    /** 使用通用权限处理器，不额外查询或装配数据源。 */
    public ParameterizedDataPermissionInterceptor() {
        super(new MyDataPermissionHandler());
    }

    /** 在分页和缓存键生成前将查询中的新增值完整绑定。 */
    @Override
    public void beforeQuery(Executor executor, MappedStatement statement, Object parameter, RowBounds rowBounds,
        ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        try (var context = DataPermissionParameterContext.open()) {
            super.beforeQuery(executor, statement, parameter, rowBounds, resultHandler, boundSql);
            context.apply(statement.getConfiguration(), boundSql);
        }
    }

    /** 更新和删除在 prepare 前改写，保证 SET 参数及 WHERE 权限参数位置一致。 */
    @Override
    public void beforePrepare(StatementHandler handler, Connection connection, Integer timeout) {
        var wrapped = PluginUtils.mpStatementHandler(handler);
        MappedStatement statement = wrapped.mappedStatement();
        if (statement.getSqlCommandType() != SqlCommandType.UPDATE && statement.getSqlCommandType() != SqlCommandType.DELETE) {
            return;
        }
        try (var context = DataPermissionParameterContext.open()) {
            super.beforePrepare(handler, connection, timeout);
            context.apply(statement.getConfiguration(), wrapped.boundSql());
        }
    }
}
