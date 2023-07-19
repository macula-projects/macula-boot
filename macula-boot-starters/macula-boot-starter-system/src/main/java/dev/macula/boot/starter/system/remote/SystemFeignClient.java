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

package dev.macula.boot.starter.system.remote;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.starter.system.config.FeignClientConfiguration;
import dev.macula.boot.starter.system.dto.RouteVO;
import dev.macula.boot.starter.system.dto.UserLoginVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * {@code SysUserFeignClient} system模块用户服务的远程调用
 *
 * @author rain
 * @since 2023/2/20 20:31
 */
@FeignClient(value = "macula-cloud-system", url = "${macula.cloud.endpoint}", contextId = "systemFeignClient",
    configuration = FeignClientConfiguration.class)
public interface SystemFeignClient {

    @GetMapping("/system/api/v1/users/{username}/loginUserinfo")
    UserLoginVO getUserInfoWithoutRoles(@PathVariable String username,
        @RequestParam(value = GlobalConstants.TOKEN_ID_NAME, required = false) String tokenId);

    @GetMapping("/system/api/v1/menus/routes")
    List<RouteVO> listRoutes(@RequestParam(value = GlobalConstants.TOKEN_ID_NAME, required = false) String tokenId);
}
