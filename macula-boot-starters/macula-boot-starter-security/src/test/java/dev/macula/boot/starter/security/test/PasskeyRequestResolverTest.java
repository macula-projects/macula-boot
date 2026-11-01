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

package dev.macula.boot.starter.security.test;

import dev.macula.boot.starter.security.passkey.PasskeyRequestResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks strict header/query extraction without accepting form fields or ambiguous credentials.
 *
 * @author Macula
 * @since 6.1.0
 */
class PasskeyRequestResolverTest {
    private final PasskeyRequestResolver resolver = new PasskeyRequestResolver();

    @Test
    void acceptsEitherSourceAndMatchingBoth() {
        var request = new MockHttpServletRequest();
        request.setQueryString("passkey=v1.token");
        assertThat(resolver.resolve(request)).isEqualTo("v1.token");
        request.addHeader("X-Resource-Passkey", "v1.token");
        assertThat(resolver.resolve(request)).isEqualTo("v1.token");
        request.setQueryString(null);
        assertThat(resolver.resolve(request)).isEqualTo("v1.token");
    }

    @Test
    void rejectsDuplicatesConflictsAndInvalidEncoding() {
        for (String query : new String[] {"passkey=a&passkey=a", "passkey=a&%70asskey=a", "passkey=%xx", "passkey="}) {
            var request = new MockHttpServletRequest();
            request.setQueryString(query);
            assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(RuntimeException.class);
        }
        var request = new MockHttpServletRequest();
        request.setQueryString("passkey=b");
        request.addHeader("X-Resource-Passkey", "a");
        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(RuntimeException.class);
        request.setQueryString(null);
        request.addHeader("X-Resource-Passkey", "a");
        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsFormOnlyOrMissingRequest() {
        var request = new MockHttpServletRequest();
        request.addParameter("passkey", "form-only");
        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(RuntimeException.class);
    }
}
