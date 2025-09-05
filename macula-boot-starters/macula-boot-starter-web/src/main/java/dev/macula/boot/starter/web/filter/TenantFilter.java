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

package dev.macula.boot.starter.web.filter;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.TenantContextHolder;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * {@code TenantFilter} 提取租户ID参数放入上下文，需要检验当前用户是否是租户负责人
 *
 * @author rain
 * @since 2023/3/1 18:47
 */
@WebFilter(urlPatterns = "/*")
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest)request;
        String tenantId = req.getHeader(GlobalConstants.TENANT_ID_NAME);
        if (StrUtil.isEmpty(tenantId)) {
            tenantId = req.getParameter(GlobalConstants.TENANT_ID_NAME);
        }

        // 设置租户ID上下文
        if (StrUtil.isNotEmpty(tenantId)) {
            TenantContextHolder.setCurrentTenantId(Long.parseLong(tenantId));
        } else {
            TenantContextHolder.clearCurrentTenantId();
        }

        chain.doFilter(request, response);
    }
}
