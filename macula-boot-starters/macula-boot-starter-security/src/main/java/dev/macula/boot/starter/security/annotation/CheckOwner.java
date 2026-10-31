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

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks that a synchronous single-object query returns data owned by the current authenticated principal.
 * <p>
 * The owner expression must use the same identity and compatible type as {@code authentication.name}.
 * Checks run through Spring method-security proxies, before response-body advice wraps the result.
 * This annotation does not filter collections, enforce tenant boundaries or prevent method side effects.
 *
 * @author Macula
 * @since 6.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("isAuthenticated()")
@PostAuthorize("returnObject != null and ({value}) != null and ({value}) == authentication.name")
public @interface CheckOwner {

    /**
     * SpEL expression resolving the owner from the actual method result, for example
     * {@code returnObject.buyerUsername} or {@code returnObject.data?.buyerUsername}.
     * Null results and null owners are denied; invalid expressions must be corrected by the application.
     *
     * @return the required owner expression
     */
    String value();
}
