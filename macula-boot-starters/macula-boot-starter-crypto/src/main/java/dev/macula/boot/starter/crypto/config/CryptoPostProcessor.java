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

package dev.macula.boot.starter.crypto.config;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * {@code CryptoPostProcessor} 配置文件属性加解密处理
 *
 * @author rain
 * @since 2023/3/21 17:49
 */
public class CryptoPostProcessor implements EnvironmentPostProcessor {

    private static final String PREFIX = "mpw:";
    private static final String PASSWORD_KEY = "macula.crypto.key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        /**
         * 命令行中获取密钥
         */
        String key = null;
        for (PropertySource<?> ps : environment.getPropertySources()) {
            if (ps.containsProperty(PASSWORD_KEY)) {
                key = ps.getProperty(PASSWORD_KEY).toString();
                break;
            }
        }

        /**
         * 处理加密内容
         */
        if (StrUtil.isNotBlank(key)) {
            HashMap<String, Object> map = new HashMap<>();
            for (PropertySource<?> ps : environment.getPropertySources()) {
                if (ps instanceof OriginTrackedMapPropertySource) {
                    OriginTrackedMapPropertySource source = (OriginTrackedMapPropertySource)ps;
                    for (String name : source.getPropertyNames()) {
                        Object value = source.getProperty(name);
                        if (value instanceof String) {
                            String str = (String)value;
                            if (str.startsWith(PREFIX)) {
                                map.put(name,
                                    SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8)).decryptStr(str.substring(4)));
                            }
                        }
                    }
                }
            }

            // 将解密的数据放入环境变量，并处于第一优先级上
            if (CollectionUtil.isNotEmpty(map)) {
                environment.getPropertySources().addFirst(new MapPropertySource("custom-encrypt", map));
            }
        }
    }
}
