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

package org.apache.rocketmq.mysql.mq;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.mysql.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class RocketMQMySqlProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQMySqlProducer.class);

    private DefaultMQProducer producer;
    private Config config;

    public RocketMQMySqlProducer(Config config) {
        this.config = config;
    }

    public void start() throws MQClientException {

        // 是否配置了AK/SK
        boolean isEnableAcl = !StringUtils.isEmpty(config.mqAccessKey) && !StringUtils.isEmpty(config.mqSecretKey);

        if (isEnableAcl) {
            producer = new DefaultMQProducer(new AclClientRPCHook(new SessionCredentials(config.mqAccessKey, config.mqSecretKey)));
        } else {
            producer = new DefaultMQProducer();
        }
        producer.setNamesrvAddr(config.mqNamesrvAddr);
        producer.setNamespace(config.mqNamespace);
        producer.setProducerGroup(config.mqProducerGroup);
        producer.start();
    }

    public long push(String json) throws Exception {
        LOGGER.debug(json);

        Message message = new Message(config.mqTopic, json.getBytes("UTF-8"));
        SendResult sendResult = producer.send(message);

        return sendResult.getQueueOffset();
    }
}