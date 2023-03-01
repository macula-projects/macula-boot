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

package dev.macula.boot.starter.sender.support;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.sender.Message;
import lombok.Data;

import java.util.Date;

/**
 * {@code LocalMessage} 本地消息
 *
 * @author https://gitee.com/litao851025/lego
 * @since 2023/1/3 14:10
 */
@Data
public class LocalMessage {
    public static final int STATUS_NONE = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_ERROR = 2;
    private static final int MAX_RETRY_TIME = 3;
    private Long id;

    private boolean orderly;

    private String topic;

    private String shardingKey;

    private String tag;

    private String msgKey;

    private String msgId;

    private String msg;

    private int retryTime = 0;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    public static LocalMessage apply(Message message) {
        if (message == null) {
            return null;
        }
        Assert.isTrue(StrUtil.isNotEmpty(message.getTopic()));
        Assert.isTrue(StrUtil.isNotEmpty(message.getMsg()));

        LocalMessage localMessage = new LocalMessage();
        localMessage.setOrderly(message.isOrderly());
        localMessage.setTopic(message.getTopic());
        localMessage.setShardingKey(message.getShardingKey());
        localMessage.setTag(message.getTag());
        localMessage.setMsg(message.getMsg());
        localMessage.setMsgKey(message.getMsgKey());

        localMessage.init();

        return localMessage;
    }

    private void init() {
        setStatus(STATUS_NONE);
        if (StrUtil.isEmpty(getShardingKey())) {
            setShardingKey("");
        }
        if (StrUtil.isEmpty(getTag())) {
            setTag("");
        }
        if (StrUtil.isEmpty(getMsgKey())) {
            setMsgKey("");
        }

        setMsgId("");
        setCreateTime(new Date());
        setUpdateTime(new Date());
    }

    public boolean needRetry(Date now) {
        return this.status != STATUS_SUCCESS //未成功
            && this.retryTime < MAX_RETRY_TIME //未达到最大尝试次数
            && now.getTime() - updateTime.getTime() > 10 * 1000; //更新时间在10秒外
    }

    public void sendSuccess(String msgId) {
        this.setMsgId(msgId);
        this.setStatus(STATUS_SUCCESS);
        this.setUpdateTime(new Date());
    }

    public void sendError() {
        this.setStatus(STATUS_ERROR);
        this.retryTime = this.retryTime + 1;
        this.setUpdateTime(new Date());
    }

    public Message toMessage() {
        Message message = new Message();
        message.setOrderly(isOrderly());
        message.setTopic(getTopic());
        message.setShardingKey(getShardingKey());
        message.setTag(getTag());
        message.setMsg(getMsg());
        message.setMsgKey(getMsgKey());
        return message;
    }
}

