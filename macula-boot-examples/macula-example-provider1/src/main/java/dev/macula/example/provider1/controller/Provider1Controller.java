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

package dev.macula.example.provider1.controller;

import dev.macula.boot.starter.security.utils.SecurityUtils;
import dev.macula.example.provider1.vo.UserResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * {@code Provider1Controller} REST 接口
 *
 * @author rain
 * @since 2022/7/22 15:14
 */

@RestController
@RequestMapping("/api/v1/provider1")
@Tag(name = "提供方演示接口", description = "提供方演示")
public class Provider1Controller {
    @GetMapping("/echo")
    @Operation(summary = "echo方法", description = "用于演示")
    @Parameter(name = "字符串", description = "用于回声")
    public String echo(@RequestParam("str") String str) {
        return "Hello " + str + ", by " + SecurityUtils.getCurrentUser();
    }

    @PostMapping("/user")
    @Operation(summary = "获取用户信息", description = "用于获取用户信息")
    public UserResult getUser() {
        UserResult result = new UserResult();
        result.setUserName("Rain");
        result.setPassword("password");
        return result;
    }
}
