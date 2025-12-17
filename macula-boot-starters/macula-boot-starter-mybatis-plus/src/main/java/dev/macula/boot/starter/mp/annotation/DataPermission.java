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

package dev.macula.boot.starter.mp.annotation;

import java.lang.annotation.*;

/**
 * {@code DataPermission} MP 数据权限注解
 * <p>
 * <a href="https://gitee.com/baomidou/mybatis-plus/issues/I37I90">mybatis-plus</a>
 *
 * @author <a href="mailto:2256222053@qq.com">zc</a>
 * @author Rain
 * @since 2021-12-10 15:48
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DataPermission {

    /**
     * 部门别名
     *
     * @return 部门别名
     */
    String deptAlias() default "";

    /**
     * 部门ID列名
     *
     * @return 部门ID列名
     */
    String deptIdColumnName() default "dept_id";

    /**
     * 用户别名
     *
     * @return 用户别名
     */
    String userAlias() default "";

    /**
     * 用户ID列名
     *
     * @return 用户ID列名
     */
    String userIdColumnName() default "create_by";

}