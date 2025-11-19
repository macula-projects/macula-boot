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
 *
 * @author Gordian
 * @since 2025-11-19
 */
public final class OperationLogConstant {

    private OperationLogConstant() {
    }

    /** 操作类型 start */

    /** 增加 */
    public static final String OPERATION_TYPE_ADD = "ADD";

    /** 删除 */
    public static final String OPERATION_TYPE_DELETE = "DELETE";

    /** 更新 */
    public static final String OPERATION_TYPE_UPDATE = "UPDATE";

    /** 新增或者修改 */
    public static final String OPERATION_TYPE_SAVE_OR_UPDATE = "SAVE_OR_UPDATE";

    /** 查询 */
    public static final String OPERATION_TYPE_SELECT = "SELECT";

    /** 操作类型 end */

    /** 分层 start */

    /** 控制器层 */
    public static final String LAYER_CONTROLLER = "CONTROLLER";

    /** 业务层 */
    public static final String LAYER_SERVICE = "SERVICE";

    /** 领域层 */
    public static final String LAYER_DOMAIN = "DOMAIN";

    /** 仓储层级 */
    public static final String LAYER_REPOSITORY = "REPOSITORY";

    /** 分层 end */
}