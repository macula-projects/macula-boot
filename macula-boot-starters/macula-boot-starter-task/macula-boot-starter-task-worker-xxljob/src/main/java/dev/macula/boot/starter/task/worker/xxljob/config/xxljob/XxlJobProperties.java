/*
 * Copyright (c) 2024 Macula
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

package dev.macula.boot.starter.task.worker.xxljob.config.xxljob;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * xxl-job配置
 *
 * @author lishangbu
 * @since 2020/9/14
 */
@Data
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    @NestedConfigurationProperty
    private XxlAdminProperties admin = new XxlAdminProperties();

    @NestedConfigurationProperty
    private XxlExecutorProperties executor = new XxlExecutorProperties();

}
