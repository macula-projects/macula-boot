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

package dev.macula.boot.starter.system.controller;

import dev.macula.boot.starter.system.dto.RouteVO;
import dev.macula.boot.starter.system.dto.UserLoginVO;
import dev.macula.boot.starter.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code SystemController} system模块对接接口
 *
 * @author rain
 * @since 2023/2/21 15:59
 */
@Tag(name = "system模块对接接口")
@RestController
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @Operation(summary = "获取登录用户信息")
    @GetMapping("/api/v1/users/me")
    public UserLoginVO getLoginUserInfo() {
        // 从macula-cloud获取用户信息
        return systemService.getUseInfo();
    }

    @Operation(summary = "路由列表")
    @GetMapping("/api/v1/menus/routes")
    public List<RouteVO> listRoutes() {
        return systemService.listRoutes();
    }
}
