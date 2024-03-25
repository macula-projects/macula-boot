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

package dev.macula.boot.starter.web.config;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.macula.boot.starter.web.interceptor.GrayHandlerInterceptor;
import dev.macula.boot.starter.web.json.MappingApiJackson2HttpMessageConverter;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * {@code MaculaWebMvcConfigurer} WebMvc配置器
 *
 * @author rain
 * @since 2022/6/29 15:04
 */
@AllArgsConstructor
public class MaculaWebMvcConfigurer implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;
    private final JacksonProperties jacksonProperties;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 默认String没有办法转为java.util.Date类型
        registry.addConverter(new Converter<String, Date>() {
            @Override
            public Date convert(String source) {
                return DateUtil.parse(source.trim()).toJdkDate();
            }
        });

        // Spring MVC date format
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GrayHandlerInterceptor());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(x -> x instanceof StringHttpMessageConverter);
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        converters.removeIf(x -> x instanceof AbstractJackson2HttpMessageConverter);
        converters.add(new MappingApiJackson2HttpMessageConverter(objectMapper, jacksonProperties));
    }
}
