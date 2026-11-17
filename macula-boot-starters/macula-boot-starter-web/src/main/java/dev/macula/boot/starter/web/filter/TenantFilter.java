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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * {@code TenantFilter} 提取请求租户提示并限定上下文生命周期；MyBatis 数据访问层检查 ROOT 切换资格。
 *
 * @author rain
 * @since 2023/3/1 18:47
 */
@WebFilter(urlPatterns = "/*")
public class TenantFilter implements Filter {

    /**
     * 在当前请求内提取租户提示；是否允许切换由数据访问层检查，退出时清理上下文。
     * @param request 当前请求
     * @param response 当前响应
     * @param chain 下游过滤链
     * @throws IOException 下游 I/O 异常
     * @throws ServletException 下游 Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        // 清理覆盖转换异常和下游异常，避免容器复用线程时遗留上一次请求租户。
        try {
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
        } finally {
            TenantContextHolder.clearCurrentTenantId();
        }
    }
}
