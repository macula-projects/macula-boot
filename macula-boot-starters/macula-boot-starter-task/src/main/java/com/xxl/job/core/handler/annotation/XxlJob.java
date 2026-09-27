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

package com.xxl.job.core.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring bean method as an XXL-compatible task handler executed by SnailJob.
 *
 * @author Rain
 * @since 6.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XxlJob {

    /**
     * Returns the executor name registered with SnailJob.
     *
     * @return executor name
     */
    String value();

    /**
     * Returns the optional no-argument initialization method name.
     *
     * @return initialization method name
     */
    String init() default "";

    /**
     * Returns the optional no-argument destruction method name.
     *
     * @return destruction method name
     */
    String destroy() default "";
}
