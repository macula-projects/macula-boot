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

package dev.macula.boot.starter.rocketmq.test.service;

import dev.macula.boot.starter.rocketmq.TxMqMessage;
import dev.macula.boot.starter.rocketmq.annotation.TxMqCheck;
import dev.macula.boot.starter.rocketmq.annotation.TxMqExecute;
import dev.macula.boot.starter.rocketmq.test.vo.OrderVo;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

/**
 * {@code OrderServiceImpl} 创建订单服务
 *
 * @author rain
 * @since 2022/11/30 15:15
 */

@Service
public class OrderServiceImpl {

    private final String BIZ_NAME_ORDER = "BIZ_ORDER";
    private final String TOPIC_ORDER = "TOPIC_ORDER_TX";
    private RocketMQTemplate rocketMQTemplate;

    public OrderServiceImpl(RocketMQTemplate template) {
        this.rocketMQTemplate = template;
    }

    public void createOrderWithMq(OrderVo order) {
        TxMqMessage<OrderVo> txMsg = new TxMqMessage<>(order, this.getClass(), BIZ_NAME_ORDER, order.getOrderNo());
        rocketMQTemplate.sendMessageInTransaction(TOPIC_ORDER, txMsg, new Object[] {order});
    }

    // @Transactional,数据库事务注解
    @TxMqExecute(BIZ_NAME_ORDER)
    public void createOrder(OrderVo order) {
        System.out.println("=============" + order.getOrderNo());
    }

    @TxMqCheck(BIZ_NAME_ORDER)
    public boolean checkOrder(String orderNo) {
        System.out.println("order_no=" + orderNo);
        return true;
    }
}
