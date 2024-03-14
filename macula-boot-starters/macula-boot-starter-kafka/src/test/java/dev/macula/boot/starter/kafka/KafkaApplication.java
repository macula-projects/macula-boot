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

package dev.macula.boot.starter.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * <b>KafkaApplication</b> 启动类
 *
 * @author Rain
 * @since 2024/3/14
 */
@SpringBootApplication
public class KafkaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaApplication.class, args);
    }

    @Component
    @Slf4j
    public static class BigDataTopicListener {

        /**
         * 监听kafka数据
         *
         * @param consumerRecord 消费记录
         */
        @KafkaListener(topics = {"big_data_topic"})
        public void consumer(ConsumerRecord<?, ?> consumerRecord) {
            log.info("收到bigData推送的数据'{}'", consumerRecord.toString());
            //...
            //db.save(consumerRecord);//插入或者更新数据
        }
    }

}
