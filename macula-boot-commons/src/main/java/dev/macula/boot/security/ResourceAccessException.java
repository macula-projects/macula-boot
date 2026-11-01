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

package dev.macula.boot.security;

/**
 * Indicates that a response resource cannot be authorized or issued a credential.
 *
 * @author Macula
 * @since 6.1.0
 */
public class ResourceAccessException extends RuntimeException implements ResourceAccessDenied {

    /**
     * Creates a resource access failure without exposing credential material.
     * @param message public failure description
     */
    public ResourceAccessException(String message) {
        super(message);
    }
}
