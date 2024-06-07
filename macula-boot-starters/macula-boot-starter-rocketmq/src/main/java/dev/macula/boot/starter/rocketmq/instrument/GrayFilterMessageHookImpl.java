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

package dev.macula.boot.starter.rocketmq.instrument;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import dev.macula.boot.context.GrayVersionMetaHolder;
import dev.macula.boot.starter.rocketmq.config.GrayRocketMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.hook.FilterMessageContext;
import org.apache.rocketmq.client.hook.FilterMessageHook;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;

import java.util.Iterator;
import java.util.List;

/**
 * {@code FilterMessageHookImpl} 消息过滤
 *
 * @author rain
 * @since 2023/9/7 19:42
 */
@Slf4j
@RequiredArgsConstructor
public class GrayFilterMessageHookImpl implements FilterMessageHook {
    private final GrayRocketMQProperties grayRocketMQProperties;
    private final Environment environment;
    private final DiscoveryClient discoveryClient;

    public String hookName() {
        return "grayFilterMessageHookImpl";
    }

    public void filterMessage(FilterMessageContext context) {
        if (!this.grayRocketMQProperties.isEnabled())
            return;

        // 当前实例是否是灰度实例的标识
        String metaGrayVersion = GrayVersionMetaHolder.getGrayVersion();

        // 判断是否有基线实例
        String serviceId = environment.getProperty("spring.application.name");
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        // 只要有一个实例没有grayversion标识，则标识有基线环境
        boolean hasMainInstance = instances.stream().anyMatch(s -> !s.getMetadata().containsKey(GlobalConstants.GRAY_VERSION_TAG));

        log.debug("rocketmq consumer gray before, args: {}, group metaGrayVersion: {}", context, metaGrayVersion);
        try {
            List<MessageExt> messageList = context.getMsgList();
            Iterator<MessageExt> iterator = messageList.iterator();
            while (iterator.hasNext()) {
                MessageExt record = iterator.next();
                String messageGrayVersion = getConsumerRecordGrayVersion(record);

                // 判断不可消费的消息移除
                boolean ifConsume = ifConsume(messageGrayVersion, metaGrayVersion, hasMainInstance, instances);
                if (!ifConsume) {
                    iterator.remove();
                }
            }
            log.debug("rocketmq consumer gray after, args: {}, group metaGrayVersion: {}", context, metaGrayVersion);
        } catch (Exception e) {
            log.error("extract gray from rocketmq message error", e);
        }
    }

    private boolean ifConsume(String messageGrayVersion, String metaGrayVersion, boolean hasMainInstance, List<ServiceInstance> instances) {
        if (StrUtil.isEmpty(messageGrayVersion)) {
            // 基线消息
            if (StrUtil.isEmpty(metaGrayVersion)) {
                // 基线环境可消费
                return true;
            }
            // 基线消息，当前实例是灰度环境且不存在基线环境，配置允许灰度消费基线消息则返回true
            return !hasMainInstance && this.grayRocketMQProperties.isGrayConsumeMain();
        }

        log.debug("rocketmq message contains gray version: {}", messageGrayVersion);

        if (StrUtil.isEmpty(metaGrayVersion)) {
            // 灰度消息，当前是基线环境且不存在指定灰度环境，配置允许基线消费灰度消息则返回true

            // 查询是否存在指定灰度标识的实例
            boolean hasGrayInstance = instances.stream().anyMatch(s -> StrUtil.equals(messageGrayVersion, s.getMetadata().get(GlobalConstants.GRAY_VERSION_TAG)));

            return !hasGrayInstance && this.grayRocketMQProperties.isMainConsumeGray();
        }

        // 灰度消息，灰度环境、看是否相等来决定是否可以消费
        return messageGrayVersion.equals(metaGrayVersion);
    }

    private String getConsumerRecordGrayVersion(MessageExt record) {
        return record.getUserProperty(GlobalConstants.GRAY_VERSION_TAG);
    }
}
