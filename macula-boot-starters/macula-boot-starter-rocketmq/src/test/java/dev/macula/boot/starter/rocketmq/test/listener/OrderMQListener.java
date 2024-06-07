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

package dev.macula.boot.starter.rocketmq.test.listener;

import dev.macula.boot.context.GrayVersionContextHolder;
import dev.macula.boot.starter.rocketmq.test.vo.OrderVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * {@code OrderMQListener} MQ监听器
 *
 * @author Rain
 * @since 2024/5/17 11:41
 */
@RocketMQMessageListener(topic = "${demo.rocketmq.topic.order: TOPIC_ORDER}", consumerGroup = "demo")
@Service
@Slf4j
public class OrderMQListener implements RocketMQListener<OrderVo> {
    @Override
    public void onMessage(OrderVo order) {
        log.info("receive order: {}", order);
        System.out.println("gray = " + GrayVersionContextHolder.getGrayVersion());
    }
}
