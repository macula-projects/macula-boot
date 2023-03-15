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

package dev.macula.boot.starter.system.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.security.utils.SecurityUtils;
import dev.macula.boot.starter.system.dto.UserLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.PatternMatchUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * {@code PermissionService} 鉴权服务，用于校验按钮权限标识，可用在Controller方法的注解中
 *
 * @author rain
 * @since 2022/12/27 13:41
 */
@Service("pms")
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final SystemService systemService;

    public boolean hasPermission(String perm) {

        if (StrUtil.isBlank(perm)) {
            return false;
        }

        if (SecurityUtils.isRoot()) {
            return true;
        }

        Set<String> perms = new HashSet<>();
        try {
            UserLoginVO user = systemService.getUseInfo();
            if (user != null) {
                perms = user.getPerms();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        if (CollectionUtil.isEmpty(perms)) {
            return false;
        }
        return perms.stream().anyMatch(item -> PatternMatchUtils.simpleMatch(perm, item));
    }
}

