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
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.operators.relational.OldOracleJoinBinaryExpression;
import net.sf.jsqlparser.expression.operators.relational.SupportsOldOracleJoinSyntax;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Top;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 单次权限 SQL 改写的参数上下文，按最终 SQL 输出顺序组合原参数与新增参数。
 * 支持嵌套查询，必须由 try-with-resources 关闭，不能作为跨线程身份传递机制。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class DataPermissionParameterContext implements AutoCloseable {
    private static final ThreadLocal<DataPermissionParameterContext> CURRENT = new ThreadLocal<>();
    private final DataPermissionParameterContext previous;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final String prefix = "__macula_permission_" + UUID.randomUUID().toString().replace("-", "") + "_";
    private boolean closed;

    /** 创建并保存外层上下文，避免嵌套 Mapper 覆盖外层绑定。 */
    private DataPermissionParameterContext() {
        this.previous = CURRENT.get();
        CURRENT.set(this);
    }

    /**
     * 开始一次 SQL 改写；拦截器需在 finally 中恢复外层上下文。
     * @return 可关闭的当前参数上下文
     */
    public static DataPermissionParameterContext open() {
        return new DataPermissionParameterContext();
    }

    /**
     * 注册待绑定值，返回不会携带原始值的临时命名参数。
     * @param value 非 null 的 JDBC 参数
     * @return 仅用于本次 AST 改写的标记
     */
    public static JdbcNamedParameter bind(Object value) {
        DataPermissionParameterContext context = current();
        String name = context.prefix + context.values.size();
        context.values.put(name, Objects.requireNonNull(value));
        return new JdbcNamedParameter(name);
    }

    /** 获取当前上下文；原生 DataPermissionInterceptor 不支持本处理器的参数绑定。 */
    private static DataPermissionParameterContext current() {
        DataPermissionParameterContext context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("数据权限值必须通过 ParameterizedDataPermissionInterceptor 绑定");
        }
        return context;
    }

    /**
     * 以最终输出顺序交织原参数，保留 foreach 等已有 additionalParameters。
     * @param configuration 当前 Mapper 配置
     * @param boundSql 已追加权限条件的 SQL
     */
    public void apply(Configuration configuration, BoundSql boundSql) {
        if (values.isEmpty()) {
            return;
        }
        List<ParameterMapping> original = boundSql.getParameterMappings() == null ? List.of() : boundSql.getParameterMappings();
        var sql = new StringBuilder();
        var expressions = new BindingDeParser(configuration, original, sql);
        var statements = new StatementDeParser(expressions, new BindingSelectDeParser(expressions, sql), sql);
        try {
            boolean first = true;
            for (var statement : CCJSqlParserUtil.parseStatements(boundSql.getSql())) {
                if (!first) {
                    sql.append("; ");
                }
                statement.accept(statements);
                first = false;
            }
        } catch (Exception failure) {
            throw new IllegalStateException("数据权限参数 SQL 解析失败", failure);
        }
        if (expressions.originalIndex != original.size() || expressions.boundCount != values.size()) {
            throw new IllegalStateException("无法完整匹配数据权限 SQL 的参数顺序");
        }
        var wrapped = PluginUtils.mpBoundSql(boundSql);
        wrapped.sql(sql.toString());
        wrapped.parameterMappings(expressions.mappings);
        values.forEach(boundSql::setAdditionalParameter);
    }

    /** 恢复外层上下文，最外层完成时移除 ThreadLocal。 */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (CURRENT.get() != this) {
            throw new IllegalStateException("数据权限参数上下文必须按嵌套顺序关闭");
        }
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
        closed = true;
    }

    /**
     * 补齐默认 SelectDeParser 直接转字符串的 TOP 表达式，避免遗漏其中的参数。
     * @since 6.1.0
     */
    private static final class BindingSelectDeParser extends SelectDeParser {
        private final BindingDeParser expressions;
        private final StringBuilder sql;

        /** 与表达式访问器共用输出缓冲，保持参数与最终 SQL 顺序一致。 */
        private BindingSelectDeParser(BindingDeParser expressions, StringBuilder sql) {
            super(expressions, sql);
            this.expressions = expressions;
            this.sql = sql;
        }

        /** 保留 TOP 选项，并通过参数访问器输出其表达式。 */
        @Override
        public void visit(Top top) {
            sql.append("TOP ");
            if (top.hasParenthesis()) {
                sql.append('(');
            }
            top.getExpression().accept(expressions, null);
            if (top.hasParenthesis()) {
                sql.append(')');
            }
            if (top.isPercentage()) {
                sql.append(" PERCENT");
            }
            if (top.isWithTies()) {
                sql.append(" WITH TIES");
            }
            sql.append(' ');
        }
    }

    /**
     * 在 SQL 反解析时记录真正占位符的顺序，字符串和注释中的问号不作为参数。
     * @since 6.1.0
     */
    private final class BindingDeParser extends ExpressionDeParser {
        private final Configuration configuration;
        private final List<ParameterMapping> original;
        private final List<ParameterMapping> mappings = new ArrayList<>();
        private int originalIndex;
        private int boundCount;

        /** 使用同一个输出缓冲和配置创建参数映射。 */
        private BindingDeParser(Configuration configuration, List<ParameterMapping> original, StringBuilder sql) {
            super(null, sql);
            this.configuration = configuration;
            this.original = original;
        }

        /** 默认实现直接转字符串；这里按原子句顺序访问层级条件中的参数。 */
        @Override
        public <S> StringBuilder visit(OracleHierarchicalExpression hierarchy, S context) {
            if (!hierarchy.isConnectFirst()) {
                appendStart(hierarchy, context);
            }
            builder.append(" CONNECT BY ");
            if (hierarchy.isNoCycle()) {
                builder.append("NOCYCLE ");
            }
            hierarchy.getConnectExpression().accept(this, context);
            if (hierarchy.isConnectFirst()) {
                appendStart(hierarchy, context);
            }
            return builder;
        }

        /** START WITH 可缺省，有值时必须通过参数访问器输出。 */
        private <S> void appendStart(OracleHierarchicalExpression hierarchy, S context) {
            if (hierarchy.getStartExpression() != null) {
                builder.append(" START WITH ");
                hierarchy.getStartExpression().accept(this, context);
            }
        }

        /** 默认比较式反解析会丢弃 PRIOR，保留其位置及旧 Oracle 外连接标记。 */
        @Override
        public <S> StringBuilder deparse(OldOracleJoinBinaryExpression expression, String operator, S context) {
            if (expression.getOraclePriorPosition() == SupportsOldOracleJoinSyntax.ORACLE_PRIOR_START) {
                builder.append("PRIOR ");
            }
            expression.getLeftExpression().accept(this, context);
            if (expression.getOldOracleJoinSyntax() == SupportsOldOracleJoinSyntax.ORACLE_JOIN_RIGHT) {
                builder.append("(+)");
            }
            builder.append(operator);
            if (expression.getOraclePriorPosition() == SupportsOldOracleJoinSyntax.ORACLE_PRIOR_END) {
                builder.append("PRIOR ");
            }
            expression.getRightExpression().accept(this, context);
            if (expression.getOldOracleJoinSyntax() == SupportsOldOracleJoinSyntax.ORACLE_JOIN_LEFT) {
                builder.append("(+)");
            }
            return builder;
        }

        /** 原 JDBC 参数保持原映射和值，仅按输出位置移动。 */
        @Override
        public <S> StringBuilder visit(JdbcParameter parameter, S context) {
            if (originalIndex >= original.size()) {
                throw new IllegalStateException("原 SQL 参数映射缺失");
            }
            mappings.add(original.get(originalIndex++));
            return super.visit(parameter, context);
        }

        /** 本次命名标记转换为问号，并绑定与值类型一致的 ParameterMapping。 */
        @Override
        public <S> StringBuilder visit(JdbcNamedParameter parameter, S context) {
            String name = parameter.getName();
            if (!values.containsKey(name)) {
                return super.visit(parameter, context);
            }
            mappings.add(new ParameterMapping.Builder(configuration, name, values.get(name).getClass()).build());
            boundCount++;
            builder.append('?');
            return builder;
        }
    }
}
