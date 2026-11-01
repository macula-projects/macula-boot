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
 * Response processing extension for issuing a credential using a trusted current identity.
 * Implementations must reject unauthenticated calls and invalid signing configuration.
 *
 * @author Macula
 * @since 6.1.0
 */
@FunctionalInterface
public interface PasskeyIssuer {

    /**
     * Assigns a fresh credential to an already authorized list record.
     * @param resource authorized record
     */
    void issue(PasskeyResource resource);
}
