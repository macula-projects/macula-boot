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

package dev.macula.example.consumer.feign.fallback;

import dev.macula.example.consumer.feign.Provider1Service;
import dev.macula.example.consumer.vo.UserResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@code Provider1FallbackFactory} is 降级演示
 *
 * @author rain
 * @since 2023/7/12 12:04
 */
@Component
@Slf4j
public class Provider1FallbackFactory extends AbstractProviderFallbackFactory {

    @Override
    public Provider1Service create(Throwable cause) {
        log.error("异常原因:{}", cause.getMessage(), cause);
        return new Provider1Service() {
            @Override
            public String echo(String str) {
                return "Degrade " + str;
            }

            @Override
            public UserResult getUser() {
                return null;
            }
        };
    }
}
