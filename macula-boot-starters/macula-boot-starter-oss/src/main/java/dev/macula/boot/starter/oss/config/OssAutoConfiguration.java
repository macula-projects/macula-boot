/*
 * Copyright (c) 2022 Macula
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

/*
 * 爱组搭 http://aizuda.com 低代码组件化开发平台
 * ------------------------------------------
 * 受知识产权保护，请勿删除版权申明
 */
package dev.macula.boot.starter.oss.config;

import dev.macula.boot.starter.oss.IFileStorage;
import dev.macula.boot.starter.oss.utils.SpringUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * aizuda oss 自动装配
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品
 *
 * @author izyao
 * @since 2022/3/22
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    private final OssProperties ossProperties;

    public OssAutoConfiguration(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @Bean
    public IFileStorage fileStorage(ApplicationContext applicationContext) {
        Map<String, OssProperty> oss = ossProperties.getOss();
        if (ObjectUtils.isEmpty(oss)) {
            throw new BeanInitializationException("oss init error");
        }
        SpringUtils.setApplicationContext(applicationContext);
        oss.forEach((k, v) -> {
            try {
                if (null == OssProperties.DEFAULT_PLATFORM) {
                    // 第一个配置为默认存储
                    OssProperties.DEFAULT_PLATFORM = k;
                }
                Class clazz = v.getPlatform().getStrategyClass();
                Constructor constructor = clazz.getConstructor(OssProperty.class);
                SpringUtils.registerSingletonBean(k, constructor.newInstance(v));
            } catch (Exception e) {
                throw new BeanInitializationException("register bean error", e);
            }
        });
        return SpringUtils.getBean(OssProperties.DEFAULT_PLATFORM, IFileStorage.class);
    }
}
