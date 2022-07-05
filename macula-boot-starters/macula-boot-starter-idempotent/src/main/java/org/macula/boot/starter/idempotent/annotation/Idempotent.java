/*
 * Copyright (c) 2022 Macula
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

package org.macula.boot.starter.idempotent.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author ITyunqing
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * <p>
     * 如果是实体类的话,默认拦截不会生效. objects.toString()会返回不同地址.
     * </p>
     * 幂等操作的唯一标识，使用spring el表达式 用#来引用方法参数
     *
     * @return Spring-EL expression
     */
    String key() default "";

    /**
     * 有效期 默认：1 有效期要大于程序执行时间，否则请求还是可能会进来
     *
     * @return expireTime
     */
    int expireTime() default 1;

    /**
     * 时间单位 默认：s
     *
     * @return TimeUnit
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 提示信息，可自定义
     *
     * @return String
     */
    String info() default "重复请求，请稍后重试";

    /**
     * 是否在业务完成后删除key true:删除 false:不删除
     *
     * @return boolean
     */
    boolean delKey() default false;

}
