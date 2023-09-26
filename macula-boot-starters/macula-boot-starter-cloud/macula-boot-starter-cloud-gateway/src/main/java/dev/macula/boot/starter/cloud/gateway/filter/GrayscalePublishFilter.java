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

package dev.macula.boot.starter.cloud.gateway.filter;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * {@code GrayscalePublishFilter} is
 *
 * @author rain
 * @since 2023/9/25 19:29
 */
@ConditionalOnProperty(value = "macula.cloud.gray.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class GrayscalePublishFilter implements GlobalFilter, Ordered {
    private final GatewayProperties gatewayProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            // TODO 这里可以扩充网关转灰度标签的规则，现在是直接在请求头放了灰度标签
            GrayVersionContextHolder.clear();
            if (gatewayProperties.getGray().isEnabled()) {
                HttpHeaders headers = exchange.getRequest().getHeaders();
                if (headers.containsKey(GlobalConstants.GRAY_VERSION_TAG)) {
                    List<String> grayValues = headers.get(GlobalConstants.GRAY_VERSION_TAG);
                    if (!Objects.isNull(grayValues) && !grayValues.isEmpty()) {
                        String grayVersion = grayValues.get(0);
                        GrayVersionContextHolder.setGrayVersion(grayVersion);
                    }
                }

                if (StrUtil.isNotEmpty(GrayVersionContextHolder.getGrayVersion())) {
                    ServerHttpRequest newRequest = exchange.getRequest().mutate()
                        .header(GlobalConstants.GRAY_VERSION_TAG, GrayVersionContextHolder.getGrayVersion()).build();
                    ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
                    return chain.filter(newExchange);
                }
            }
            return chain.filter(exchange);
        } finally {
            GrayVersionContextHolder.clear();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
