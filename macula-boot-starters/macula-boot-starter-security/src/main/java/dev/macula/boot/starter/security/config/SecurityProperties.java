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

package dev.macula.boot.starter.security.config;

import cn.hutool.core.util.ReUtil;
import cn.hutool.extra.spring.SpringUtil;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.security.annotation.Inner;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.regex.Pattern;

/**
 * {@code SecurityProperties} is
 *
 * @author rain
 * @since 2023/7/4 17:02
 */
@Slf4j
@ConfigurationProperties(prefix = "macula.security")
public class SecurityProperties implements InitializingBean {
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    private static final List<String> DEFAULT_IGNORE_URLS = SecurityConstants.DEFAULT_IGNORE_URLS;

    @Value("${spring.security.oauth2.resourceserver.jwt.secret:macula_secret$terces_alucam$123456}")
    @Setter
    @Getter
    private String jwtSecret;

    @Setter
    @Getter
    private List<String> ignoreUrls = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        ignoreUrls.addAll(DEFAULT_IGNORE_URLS);
        RequestMappingHandlerMapping mapping = SpringUtil.getBean("requestMappingHandlerMapping");
        if (mapping != null) {
            Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

            map.keySet().forEach(info -> {
                HandlerMethod handlerMethod = map.get(info);

                // 获取方法上边的注解 替代path variable 为 *
                Inner method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Inner.class);
                Optional.ofNullable(method).ifPresent(
                    inner -> Objects.requireNonNull(info.getPathPatternsCondition()).getPatternValues()
                        .forEach(url -> ignoreUrls.add(ReUtil.replaceAll(url, PATTERN, "*"))));

                // 获取类上边的注解, 替代path variable 为 *
                Inner controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Inner.class);
                Optional.ofNullable(controller).ifPresent(
                    inner -> Objects.requireNonNull(info.getPathPatternsCondition()).getPatternValues()
                        .forEach(url -> ignoreUrls.add(ReUtil.replaceAll(url, PATTERN, "*"))));
            });
        }
    }
}
