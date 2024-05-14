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
import dev.macula.boot.context.GrayVersionMetaHolder;
import dev.macula.boot.starter.rocketmq.config.GrayRocketMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.hook.FilterMessageContext;
import org.apache.rocketmq.client.hook.FilterMessageHook;
import org.apache.rocketmq.common.message.MessageExt;

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

    public String hookName() {
        return "grayFilterMessageHookImpl";
    }

    public void filterMessage(FilterMessageContext context) {
        if (!this.grayRocketMQProperties.isEnabled())
            return;

        String grayVersion = GrayVersionMetaHolder.getGrayVersion();

        log.debug("rocketmq consumer gray before, args: {}, group grayVersion: {}", context, grayVersion);
        try {
            List<MessageExt> messageList = context.getMsgList();
            Iterator<MessageExt> iterator = messageList.iterator();
            while (iterator.hasNext()) {
                MessageExt record = iterator.next();
                String messageGrayVersion = getConsumerRecordGrayVersion(record);
                // 判断不可消费的消息移除
                boolean ifConsume = ifConsume(messageGrayVersion, grayVersion);
                if (!ifConsume) {
                    iterator.remove();
                }
            }
            log.debug("rocketmq consumer gray after, args: {}, group grayVersion: {}", context, grayVersion);
        } catch (Exception e) {
            log.error("extract gray from rocketmq message error", e);
        }
    }

    private boolean ifConsume(String messageGrayVersion, String myGrayVersion) {
        // TODO 有漏洞，应该是各自环境不存在的情况下才可以根据配置去消费其他环境的消息 @2024.5.13
        if (StrUtil.isEmpty(messageGrayVersion)) {
            // 基线消息
            if (StrUtil.isEmpty(myGrayVersion)) {
                // 基线环境可消费
                return true;
            }
            // 灰度环境，看看是否灰度可以消费基线
            return this.grayRocketMQProperties.isGrayConsumeMain();
        }

        if (StrUtil.isEmpty(myGrayVersion)) {
            // 灰度消息，基线环境
            if (this.grayRocketMQProperties.isMainConsumeGray()) {
                // 基线环境消费灰度消息，把灰度消息的tag设置上下文
                GrayVersionMetaHolder.setGrayVersion(messageGrayVersion);
                return true;
            }
            // 基线环境不可以消费灰度消息
            return false;
        }

        // 灰度消息，灰度环境、看是否相等来决定是否可以消费
        return messageGrayVersion.equals(myGrayVersion);
    }

    private String getConsumerRecordGrayVersion(MessageExt record) {
        return record.getUserProperty(GlobalConstants.GRAY_VERSION_TAG);
    }
}
