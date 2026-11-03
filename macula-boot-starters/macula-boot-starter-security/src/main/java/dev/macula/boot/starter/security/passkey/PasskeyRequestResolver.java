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

import dev.macula.boot.starter.security.authorization.PasskeyAccessDeniedException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Resolves a credential exclusively from its request header or URL query, rejecting ambiguous values.
 *
 * @author Rain
 * @since 5.0.21
 */
public class PasskeyRequestResolver {

    /**
     * Resolves the credential, never reading form parameters.
     * @param request current servlet request
     * @return one non-empty credential
     * @throws PasskeyAccessDeniedException when the request is missing or ambiguous
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            throw denied();
        }
        List<String> headers = Collections.list(request.getHeaders("X-Resource-Passkey"));
        String query = null;
        boolean found = false;
        String raw = request.getQueryString();
        if (raw != null) {
            try {
                MultiValueMap<String, String> values = UriComponentsBuilder.fromPath("").query(raw).build()
                    .getQueryParams();
                for (Map.Entry<String, List<String>> entry : values.entrySet()) {
                    if ("passkey".equals(UriUtils.decode(entry.getKey(), StandardCharsets.UTF_8))) {
                        for (String value : entry.getValue()) {
                            if (found) {
                                throw denied();
                            }
                            found = true;
                            query = value == null ? null : UriUtils.decode(value, StandardCharsets.UTF_8);
                        }
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw denied();
            }
        }
        if (headers.size() > 1) {
            throw denied();
        }
        String header = headers.isEmpty() ? null : headers.get(0);
        if ((!headers.isEmpty() && !StringUtils.hasText(header)) || (found && !StringUtils.hasText(query))
            || (!headers.isEmpty() && found && !header.equals(query))) {
            throw denied();
        }
        String result = headers.isEmpty() ? query : header;
        if (!StringUtils.hasText(result) || result.length() > 128) {
            throw denied();
        }
        return result;
    }

    private PasskeyAccessDeniedException denied() {
        return new PasskeyAccessDeniedException("Invalid resource credential");
    }
}
