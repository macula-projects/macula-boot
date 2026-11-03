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

package dev.macula.boot.starter.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在 Spring MVC 参数绑定完成后，校验请求凭证与参数对应的资源编号。
 * <p>
 * 空路径表示参数本身就是资源编号，非空路径表示从已绑定的 DTO 中读取属性。
 * 校验失败时 Controller 不执行；此注解不保护 Service 方法或内部直接调用。
 * 返回值鉴权使用 PostAuthorize，操作权限使用 PreAuthorize。
 *
 * @author Rain
 * @since 6.1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Passkey {

    /**
     * JavaBean 属性路径，例如 {@code orderId} 或 {@code order.id}；留空则直接使用参数值。
     * 资源编号只支持非空字符串或整数；这里不是 SpEL，不使用 {@code #p0} 等表达式。
     *
     * @return 资源编号属性路径，默认为空
     */
    String value() default "";
}
