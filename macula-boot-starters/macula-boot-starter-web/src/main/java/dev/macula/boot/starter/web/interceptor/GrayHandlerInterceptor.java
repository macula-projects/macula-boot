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

package dev.macula.boot.starter.web.interceptor;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@code GrayHandlerInterceptor} 获取请求拦截器，用于读取灰度头设置上下文
 *
 * @author rain
 * @since 2023/9/25 19:19
 */
public class GrayHandlerInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String grayVersion = request.getHeader(GlobalConstants.GRAY_VERSION_TAG);
        // 假如HttpHeader中灰度信息，就放到上下文
        if (StrUtil.isNotEmpty(grayVersion)) {
            GrayVersionContextHolder.setGrayVersion(grayVersion);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
        ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
        Exception ex) {
        GrayVersionContextHolder.clear();
    }
}
