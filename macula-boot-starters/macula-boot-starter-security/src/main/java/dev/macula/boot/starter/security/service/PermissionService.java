/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * {@code PermissionService} 鉴权服务，用于校验按钮权限标识，可用在Controller方法的注解中
 *
 * @author rain
 * @since 2022/12/27 13:41
 */
@Service("pms")
@RequiredArgsConstructor
public class PermissionService {

    // TODO 这里的redis要指向system模块
//    private final RedisTemplate redisTemplate;
//
//    public boolean hasPermission(String perm) {
//
//        if (StrUtil.isBlank(perm)) {
//            return false;
//        }
//
//        if (SecurityUtils.isRoot()) {
//            return true;
//        }
//
//        String userName = SecurityUtils.getCurrentUser();
//
//        Set<String> perms = (Set<String>) redisTemplate.opsForValue().get(GlobalConstants.AUTH_USER_PERMS_KEY + userName);
//
//        if (CollectionUtil.isEmpty(perms)) {
//            return false;
//        }
//        return perms.stream().anyMatch(item -> PatternMatchUtils.simpleMatch(perm, item));
//    }
}

