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

package dev.macula.boot.starter.rocketmq;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;

/**
 * {@code DefaultRocketMQListenerContainerProxy} 定制化 DefaultRocketMQListenerContainer，处理消息的灰度头
 *
 * @author Rain
 * @since 2024/6/7 17:08
 */
@Slf4j
public class DefaultRocketMQListenerContainerProxy extends DefaultRocketMQListenerContainer {

    public DefaultRocketMQListenerContainerProxy(DefaultRocketMQListenerContainer container) {
        BeanUtil.copyProperties(container, this);
    }

    @Override
    public void handleMessage(MessageExt messageExt) throws MQClientException, RemotingException, InterruptedException {
        // 灰度消息，设置上下文以便在listener的onMessage方法里面发起远程访问时能够优先选中灰度实例
        String messageGrayVersion = messageExt.getUserProperty(GlobalConstants.GRAY_VERSION_TAG);
        if (StrUtil.isNotEmpty(messageGrayVersion)) {
            GrayVersionContextHolder.setGrayVersion(messageGrayVersion);
            log.debug("rocketmq message contains gray version: {}", messageGrayVersion);
        }
        super.handleMessage(messageExt);
    }
}
