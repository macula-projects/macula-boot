/*
 * Copyright 2004-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.starter.jpa.domain.support;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.security.utils.SecurityUtils;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Stub implementation for {@link AuditorAware}. Returns {@literal null} for the current auditor.
 *
 * @author Oliver Gierke
 */
public class AuditorAwareStub implements AuditorAware<String> {

    public static String getCurrentUser() {
        String name = SecurityUtils.getCurrentUser();
        if (StrUtil.isEmpty(name)) {
            name = SecurityConstants.BACKGROUND_USER;
        }
        return name;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        // 获取当前用户登录名
        return Optional.of(getCurrentUser());
    }
}
