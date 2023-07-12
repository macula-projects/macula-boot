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

package dev.macula.example.consumer.feign;

import dev.macula.example.consumer.feign.fallback.AbstractProviderFallbackFactory;
import dev.macula.example.consumer.vo.UserResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@code Provider1Service} 调用Provider1服务
 *
 * @author rain
 * @since 2022/7/22 22:33
 */
@FeignClient(name = "macula-example-provider1", path = "/api/v1/provider1", contextId = "provider1Service",
    fallbackFactory = AbstractProviderFallbackFactory.class)
public interface Provider1Service {

    @GetMapping("/echo")
    String echo(@RequestParam("str") String str);

    @PostMapping("/user")
    UserResult getUser();
}
