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

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service1.api;

import ${package}.service1.api.fallback.AbstractEchoFeignFallbackFactory;
import ${package}.service1.vo.app.ApplicationVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@code EchoFeignClient} Echo服务的封装
 *
 * @author rain
 * @since 2023/8/29 11:40
 */
@FeignClient(value = "${rootArtifactId}-service1", contextId = "echoFeignClient",
    fallbackFactory = AbstractEchoFeignFallbackFactory.class)
public interface EchoFeignClient {
    @GetMapping("/api/v1/echo/hello")
    String hello(@RequestParam("echo") String echo);

    @PostMapping("/api/v1/echo/app")
    ApplicationVO app(@RequestBody ApplicationVO vo);
}
