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

package dev.macula.boot.starter.operationlog;

/**
 * 操作日志常量类
 * <p>
 * 提供操作日志系统中使用的标准常量定义，包括操作类型和业务层级。
 * 建议在 {@link OperationLog} 注解中使用这些常量以保持一致性。
 * </p>
 *
 * <h2>使用示例：</h2>
 * <pre>{@code
 * @OperationLog(
 *     operation = TYPE_SELECT,
 *     scope = SCOPE_CONTROLLER,
 *     module = "用户管理",
 *     description = "查询用户列表"
 * )
 * }</pre>
 *
 * @author Gordian
 * @since 2025-11-19
 */
public final class OperationLogConstant {

    /**
     * 私有构造函数，防止实例化
     */
    private OperationLogConstant() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== 操作类型常量 ====================

    /**
     * 查询操作
     * <p>
     * 用于表示读取、查询、检索等不修改系统数据的操作
     * </p>
     */
    public static final String TYPE_SELECT = "SELECT";

    /**
     * 插入操作
     * <p>
     * 用于表示新增、创建、插入等添加新数据的操作
     * </p>
     */
    public static final String TYPE_INSERT = "INSERT";

    /**
     * 更新操作
     * <p>
     * 用于表示修改、更新、编辑等修改已有数据的操作
     * </p>
     */
    public static final String TYPE_UPDATE = "UPDATE";

    /**
     * 删除操作
     * <p>
     * 用于表示删除、移除等清理数据的操作
     * </p>
     */
    public static final String TYPE_DELETE = "DELETE";

    /**
     * 新增或更新操作
     * <p>
     * 用于表示根据数据存在情况决定新增或更新的操作
     * </p>
     */
    public static final String TYPE_UPSERT = "UPSERT";

    // ==================== 业务范围常量 ====================

    /**
     * 控制器范围
     * <p>
     * 表示在 Controller 层记录的操作日志，通常包含 HTTP 请求信息
     * </p>
     */
    public static final String SCOPE_CONTROLLER = "CONTROLLER";

    /**
     * 业务范围
     * <p>
     * 表示在 Service 层记录的操作日志，关注业务逻辑执行
     * </p>
     */
    public static final String SCOPE_SERVICE = "SERVICE";

    /**
     * 领域范围
     * <p>
     * 表示在 Domain 层记录的操作日志，关注领域规则和核心业务逻辑
     * </p>
     */
    public static final String SCOPE_DOMAIN = "DOMAIN";

    /**
     * 仓储范围
     * <p>
     * 表示在 Repository 层记录的操作日志，关注数据访问操作
     * </p>
     */
    public static final String SCOPE_REPOSITORY = "REPOSITORY";

}