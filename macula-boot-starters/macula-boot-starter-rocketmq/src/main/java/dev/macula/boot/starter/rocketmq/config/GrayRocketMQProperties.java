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

package dev.macula.boot.starter.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code RocketMQProperties} RocketMQ自定义配置
 *
 * @author rain
 * @since 2023/9/9 00:51
 */
@ConfigurationProperties(prefix = "macula.rocketmq.gray")
@Data
public class GrayRocketMQProperties {

    /** 是否开启按泳道生产和消费MQ消息 */
    private boolean enabled = false;

    /** 灰度实例是否消费基线实例消息 */
    private boolean grayConsumeMain = false;

    /** 基线实例是否消费灰度消息 */
    private boolean mainConsumeGray = false;
}
