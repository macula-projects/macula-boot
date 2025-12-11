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

package dev.macula.boot.starter.rocketmq;

import dev.macula.boot.starter.rocketmq.config.Constants;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code TxMessage} 事务消息
 *
 * @param <T> 消息载荷类型
 * @author rain
 * @since 2022/12/1 09:55
 */
public class TxMqMessage<T> implements Message<T>, Serializable {

    private static final long serialVersionUID = 1L;
    private final T payload;

    private final MessageHeaders headers;

    public TxMqMessage(T payload, Class<?> beanClass, String bizName, String checkId) {
        this.payload = payload;

        Map<String, Object> map = new HashMap<>();
        map.put(Constants.BEAN_CLASS_NAME, beanClass.getName());
        map.put(Constants.BIZ_NAME, bizName);
        map.put(Constants.CHECK_ID, checkId);

        this.headers = new MessageHeaders(map);
    }

    @Override
    public T getPayload() {
        return this.payload;
    }

    @Override
    public MessageHeaders getHeaders() {
        return this.headers;
    }

    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof TxMqMessage)) {
            return false;
        } else {
            TxMqMessage<?> otherMsg = (TxMqMessage<?>)other;
            return ObjectUtils.nullSafeEquals(this.payload, otherMsg.payload) && this.headers.equals(otherMsg.headers);
        }
    }

    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.payload) * 23 + this.headers.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
        sb.append(" [payload=");
        if (this.payload instanceof byte[]) {
            sb.append("byte[").append(((byte[])((byte[])this.payload)).length).append(']');
        } else {
            sb.append(this.payload);
        }

        sb.append(", headers=").append(this.headers).append(']');
        return sb.toString();
    }
}
