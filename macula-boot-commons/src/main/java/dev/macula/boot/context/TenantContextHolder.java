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

package dev.macula.boot.context;

import cn.hutool.core.thread.threadlocal.NamedThreadLocal;

/**
 * {@code TenantContextHolder} 租户ID的上下文
 *
 * @author rain
 * @since 2023/3/1 15:42
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_CONTEXT = new NamedThreadLocal<>("Tenant Context ID");

    /**
     * 获取当前上下文租户ID
     *
     * @return 租户ID
     */
    public static Long getCurrentTenantId() {
        return TENANT_CONTEXT.get();
    }

    /**
     * 设置当前租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setCurrentTenantId(Long tenantId) {
        TENANT_CONTEXT.set(tenantId);
    }

    /**
     * 清除当前租户ID
     */
    public static void clearCurrentTenantId() {
        TENANT_CONTEXT.remove();
    }
}
