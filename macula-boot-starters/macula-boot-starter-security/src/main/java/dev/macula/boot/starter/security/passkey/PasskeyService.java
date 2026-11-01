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

package dev.macula.boot.starter.security.passkey;

import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.security.PasskeyResource;
import dev.macula.boot.starter.security.authorization.CheckOwnerAccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Replaceable issuer and verifier for user-bound, globally unique resource credentials.
 *
 * @author Macula
 * @since 6.1.0
 */
public interface PasskeyService extends PasskeyIssuer {

    /**
     * Issues a read/access-scope credential after the caller has authorized the resource.
     * @param subject trusted authenticated identity
     * @param resourceId globally unique resource ID
     * @return signed, expiring credential
     */
    String issue(String subject, String resourceId);

    /**
     * Checks the signature, identity, resource and expiration.
     * @param subject trusted authenticated identity
     * @param resourceId actual operation's resource ID
     * @param passkey untrusted request credential
     * @return true only when the credential is valid
     */
    boolean verify(String subject, String resourceId, String passkey);

    @Override
    default void issue(PasskeyResource resource) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!new AuthenticationTrustResolverImpl().isAuthenticated(authentication)) {
            throw new CheckOwnerAccessDeniedException("Resource access requires authentication");
        }
        resource.setPasskey(issue(authentication.getName(), resource.getResourceId()));
    }
}
