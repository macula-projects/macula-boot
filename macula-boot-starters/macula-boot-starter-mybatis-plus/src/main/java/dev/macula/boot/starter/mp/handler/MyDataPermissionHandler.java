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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import dev.macula.boot.base.IBaseEnum;
import dev.macula.boot.enums.DataScopeEnum;
import dev.macula.boot.starter.mp.annotation.DataPermission;
import dev.macula.boot.starter.mp.utils.SecurityUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import dev.macula.boot.starter.mp.interceptor.DataPermissionParameterContext;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.lang.reflect.Method;

/**
 * {@code DataPermissionHandler} 数据权限控制器
 *
 * @author <a href="mailto:2256222053@qq.com">zc</a>
 * @since 2021-12-10 13:28
 */
@Slf4j
public class MyDataPermissionHandler implements DataPermissionHandler {

    /**
     * 根据可信身份追加数据权限；缺失或 DEFAULT 范围保留既有策略。
     * 用户名和部门 ID 由参数化拦截器的上下文绑定，不能脱离拦截器自行执行返回表达式。
     *
     * @param deptAlias 部门表别名
     * @param deptIdColumnName 部门列名
     * @param userAlias 用户表别名
     * @param userIdColumnName 用户列名
     * @param where 原查询条件，可为空
     * @return 保持原逻辑分组的数据权限条件
     * @throws IllegalArgumentException 范围或标识符非法
     */
    @SneakyThrows
    public static Expression dataScopeFilter(String deptAlias, String deptIdColumnName, String userAlias,
        String userIdColumnName, Expression where) {
        Integer dataScope = SecurityUtils.getDataScope();
        DataScopeEnum scope = dataScope == null ? DataScopeEnum.DEFAULT
            : IBaseEnum.getEnumByValue(dataScope, DataScopeEnum.class);
        if (scope == null) {
            throw new IllegalArgumentException("非法数据权限范围");
        }
        Expression permission;
        switch (scope) {
            case ALL:
            case DEFAULT:
                return where;
            case SELF:
                String username = SecurityUtils.getCurrentUser();
                if (StrUtil.isBlank(username)) {
                    permission = new EqualsTo(new LongValue(1), new LongValue(0));
                } else {
                    permission = new EqualsTo(column(userAlias, userIdColumnName), DataPermissionParameterContext.bind(username));
                }
                break;
            case DEPT:
                permission = new EqualsTo(column(deptAlias, deptIdColumnName),
                    DataPermissionParameterContext.bind(SecurityUtils.getDeptId()));
                break;
            case DEPT_AND_SUB:
                permission = departmentScope(column(deptAlias, deptIdColumnName), SecurityUtils.getDeptId());
                break;
            default:
                throw new IllegalArgumentException("不支持的数据权限范围");
        }
        // SQL AND 优先于 OR，必须分别分组，不能让原查询中的某个分支跳过权限条件。
        return where == null ? permission : new AndExpression(new ParenthesedExpressionList<>(where),
            new ParenthesedExpressionList<>(permission));
    }

    /**
     * 保留同一条 SQL 中的部门子查询，按完整逗号边界匹配，不额外访问数据源。
     * @param column 可信部门列
     * @param departmentId 认证身份中的部门 ID
     * @return 不依赖数据库专用函数的子查询条件
     */
    @SneakyThrows
    private static Expression departmentScope(Column column, Long departmentId) {
        String id = departmentId.toString();
        // 只将框架生成的参数标记写入 SQL，值由 MyBatis 绑定；不能直接拼接 tree_path 或身份值。
        return CCJSqlParserUtil.parseCondExpression(column + " IN (SELECT id FROM sys_dept WHERE id = "
            + DataPermissionParameterContext.bind(departmentId) + " OR tree_path = "
            + DataPermissionParameterContext.bind(id) + " OR tree_path LIKE "
            + DataPermissionParameterContext.bind(id + ",%") + " OR tree_path LIKE "
            + DataPermissionParameterContext.bind("%," + id + ",%") + " OR tree_path LIKE "
            + DataPermissionParameterContext.bind("%," + id) + ")");
    }

    /**
     * 仅允许注解中的简单标识符组成列引用，避免配置错误被当作 SQL 执行。
     *
     * @param alias 可为空的表别名
     * @param name 列名
     * @return 经校验的列引用
     */
    private static Column column(String alias, String name) {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")
            || (StrUtil.isNotBlank(alias) && !alias.matches("[A-Za-z_][A-Za-z0-9_]*"))) {
            throw new IllegalArgumentException("数据权限列名或别名非法");
        }
        return new Column(StrUtil.isBlank(alias) ? name : alias + StringPool.DOT + name);
    }

    /**
     * 查找 Mapper 方法的数据权限注解，ROOT 身份保持既有豁免。
     *
     * @param where 原查询条件
     * @param mappedStatementId Mapper 方法标识
     * @return 追加权限后的条件；无注解时沿用原条件
     */
    @Override
    @SneakyThrows
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 仅经认证的 ROOT 豁免；其他身份按注解和既有 dataScope 策略处理。
        if (SecurityUtils.isRoot()) {
            return where;
        }
        Class<?> clazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(StringPool.DOT)));
        String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(StringPool.DOT) + 1);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            DataPermission annotation = method.getAnnotation(DataPermission.class);
            if (ObjectUtils.isNotEmpty(annotation) && (method.getName()
                .equals(methodName) || (method.getName() + "_COUNT").equals(methodName))) {
                return dataScopeFilter(annotation.deptAlias(), annotation.deptIdColumnName(), annotation.userAlias(),
                    annotation.userIdColumnName(), where);
            }
        }
        return where;
    }
}
