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
 * Checks result ownership after a query, or verifies an input resource ID passkey before invocation.
 * <p>
 * The owner expression must use the same identity and compatible type as {@code authentication.name}.
 * Checks run through Spring method-security proxies, before response-body advice wraps the result.
 * Input expressions require a valid request passkey; result expressions cannot prevent method side effects.
 * Mixed input/result expressions are rejected. This annotation does not enforce operation or tenant permissions.
 *
 * @author Macula
 * @since 6.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckOwner {

    /**
     * SpEL expression selecting either a result owner or an input resource ID. Result examples are
     * {@code returnObject.buyerUsername} or {@code returnObject.data?.buyerUsername}.
     * Input examples are {@code #p0}, {@code #a0.orderId} or a named parameter.
     * Null results/owners and invalid expressions are denied; missing credentials never select a fallback mode.
     *
     * @return the required owner expression
     */
    String value();
}
