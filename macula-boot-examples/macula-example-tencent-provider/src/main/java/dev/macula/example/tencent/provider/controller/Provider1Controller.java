/*
 * Copyright (c) 2024 Macula
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

package dev.macula.example.tencent.provider.controller;

import cn.hutool.core.date.DateUtil;
import dev.macula.boot.starter.security.utils.SecurityUtils;
import dev.macula.example.tencent.provider.vo.UserResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * {@code Provider1Controller} REST 接口
 *
 * @author rain
 * @since 2022/7/22 15:14
 */

@RestController
@RequestMapping("/api/v1/provider1")
@Tag(name = "提供方演示接口", description = "提供方演示")
@Slf4j
public class Provider1Controller {

    @GetMapping("/echo")
    @Operation(summary = "echo方法", description = "用于演示")
    @Parameter(name = "字符串", description = "用于回声")
    public String echo(@RequestParam("str") String str) {
        log.info("echo: " + str);
        return "Hello " + str + ", by " + SecurityUtils.getCurrentUser() + ", at " + DateUtil.now();
    }

    @PostMapping("/user")
    @Operation(summary = "获取用户信息", description = "用于获取用户信息")
    public UserResult getUser(@RequestBody UserResult result) {
        result.setBirthday(new Date());
        result.setPassword("provider1_pass");
        return result;
    }
}
