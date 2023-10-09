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
package ${package}.admin.bff.service.impl;

import ${package}.admin.bff.service.EchoService;
import ${package}.service1.api.EchoFeignClient;
import ${package}.service1.vo.app.ApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@code EchoServiceImpl} ECHO 演示服务
 *
 * @author rain
 * @since 2023/8/29 13:45
 */
@Component
@RequiredArgsConstructor
public class EchoServiceImpl implements EchoService {
    private final EchoFeignClient echoFeignClient;

    @Override
    public String hello(String echo) {
        echo = echo + "xxx";
        return echoFeignClient.hello(echo);
    }

    @Override
    public ApplicationVO app(ApplicationVO vo) {
        return echoFeignClient.app(vo);
    }
}
