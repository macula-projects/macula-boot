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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A list record whose visibility authorizes the current user to access its globally unique resource.
 * Only use this contract on lists already filtered by business data permissions.
 *
 * @author Macula
 * @since 6.1.0
 */
public interface PasskeyResource {

    /**
     * Returns a stable, globally unique resource identifier without adding an auxiliary JSON property.
     * @return resource identifier
     */
    @JsonIgnore
    String getResourceId();

    /**
     * Returns the server-issued resource access credential.
     * @return passkey, or null before issuance
     */
    String getPasskey();

    /**
     * Replaces the credential for the current response's authenticated user.
     * @param passkey server-issued credential
     */
    void setPasskey(String passkey);
}
