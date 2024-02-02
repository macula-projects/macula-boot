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

package dev.macula.example.tencent.consumer.feign;

import dev.macula.example.tencent.consumer.feign.configuration.GatewayConfiguration;
import dev.macula.example.tencent.consumer.vo.UserResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@code GatewayService} 测试AK/SK请求
 *
 * @author rain
 * @since 2023/8/24 21:28
 */
@FeignClient(name = "gateway-service", url = "http://127.0.0.1:8000/consumer",
    configuration = GatewayConfiguration.class)
public interface GatewayService {
    @PostMapping("/api/v1/consumer/user")
    UserResult getUser(@RequestBody UserResult result);
}
