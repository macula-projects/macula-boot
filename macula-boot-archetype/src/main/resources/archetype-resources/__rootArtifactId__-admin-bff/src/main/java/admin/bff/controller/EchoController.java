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
package ${package}.admin.bff.controller;

import ${package}.admin.bff.service.EchoService;
import ${package}.service1.vo.app.ApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * {@code EchoController} ECHO服务
 *
 * @author rain
 * @since 2023/8/29 13:42
 */
@Tag(name = "ECHO服务", description = "ECHO服务")
@RestController
@RequestMapping("/api/v1/echo")
@RequiredArgsConstructor
public class EchoController {

    private final EchoService echoService;

    @Operation(summary = "获取hello")
    @Parameter(name = "echo字符串")
    @GetMapping(value = "/hello")
    public String hello(@RequestParam("echo") String echo) {
        return echoService.hello(echo);
    }

    @Operation(summary = "POST演示")
    @Parameter(name = "应用VO")
    @PostMapping(value = "/app")
    public ApplicationVO app(@RequestBody ApplicationVO vo) {
        return echoService.app(vo);
    }
}
