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

package dev.macula.boot.starter.system.service.impl;

import dev.macula.boot.starter.security.utils.SecurityUtils;
import dev.macula.boot.starter.system.dto.RouteVO;
import dev.macula.boot.starter.system.dto.UserLoginVO;
import dev.macula.boot.starter.system.remote.SystemFeignClient;
import dev.macula.boot.starter.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code SystemServiceImpl} system模块服务
 *
 * @author rain
 * @since 2023/2/23 17:19
 */
@Component
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final SystemFeignClient systemFeignClient;

    @Override
    public UserLoginVO getUseInfo() {
        // TODO 根据当前登录会话应该做缓存
        UserLoginVO user = systemFeignClient.getUserInfoWithoutRoles(SecurityUtils.getCurrentUser());
        if (user != null) {
            user.setRoles(SecurityUtils.getRoles());
        }
        return user;
    }

    @Override
    public List<RouteVO> listRoutes() {
        return systemFeignClient.listRoutes();
    }
}
