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

package dev.macula.boot.starter.retry.config.easyretry;

import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * {@code EnableRetry} 在启动类上添加EnableEasyRetry注解开启Easy Retry功能
 *
 * @author rain
 * @since 2023/7/24 16:03
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EasyRetryClientsRegistrar.class)
public @interface EnableEasyRetry {

    /**
     * 表示该重试数据属于哪个系统并且全局唯一
     *
     * @return 分组
     */
    String group();

    /**
     * 控制多个Aop的执行顺序, 需要注意的是这里顺序要比事务的Aop要提前 默认值: Ordered.HIGHEST_PRECEDENCE
     *
     * @return order
     */
    int order() default Ordered.HIGHEST_PRECEDENCE;
}
