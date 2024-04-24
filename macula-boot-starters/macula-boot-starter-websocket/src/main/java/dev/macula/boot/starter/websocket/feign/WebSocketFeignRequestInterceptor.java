/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.boot.starter.websocket.feign;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * <p>
 * <b>WebSocketFeignRequestInterceptor</b>有WebSocket时FeignClient额外的拦截器，处理JWT携带问题
 * </p>
 *
 * @author Rain
 * @since 2024/4/24
 */
public class WebSocketFeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // 判断是否在websocket上下文发起的feign
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth instanceof JwtAuthenticationToken) {
                String jwt = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
                if (StrUtil.isNotEmpty(jwt)) {
                    template.header(SecurityConstants.AUTHORIZATION_KEY, SecurityConstants.TOKEN_PREFIX + jwt);
                }
            }

            // 微服务之间传递的唯一标识,区分大小写所以通过httpServletRequest获取
            template.header(GlobalConstants.FEIGN_REQ_ID, String.valueOf(UUID.randomUUID()));

            // 传递灰度头给下游提供方(在SpringMVC的拦截器中设置了灰度上下文@see GrayHandlerInterceptor)
            String grayVersion = GrayVersionContextHolder.getGrayVersion();
            if (StrUtil.isNotEmpty(grayVersion)) {
                template.header(GlobalConstants.GRAY_VERSION_TAG, grayVersion);
            }
        }
    }
}
