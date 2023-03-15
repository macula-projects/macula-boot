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
 * {@code Message} 发送给MQ的消息
 *
 * @author https://gitee.com/litao851025/lego
 * @since 2023/1/3 14:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    private boolean orderly;

    private String topic;

    private String shardingKey;

    private String msgKey;

    private String tag;

    private String msg;
}
