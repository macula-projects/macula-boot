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

package dev.macula.boot.starter.sender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code Message}  发送给 MQ 的消息
 *
 * @author <a href="https://gitee.com/litao851025/lego">lego</a>
 * @since 2023/1/3 14:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    /**
     * 是否顺序消息
     */
    private boolean orderly;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 分区键
     */
    private String shardingKey;

    /**
     * 消息键
     */
    private String msgKey;

    /**
     * 消息标签
     */
    private String tag;

    /**
     * 消息内容
     */
    private String msg;
}
