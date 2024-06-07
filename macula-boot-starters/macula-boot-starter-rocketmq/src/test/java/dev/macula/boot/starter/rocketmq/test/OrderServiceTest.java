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

package dev.macula.boot.starter.rocketmq.test;

import dev.macula.boot.context.GrayVersionMetaHolder;
import dev.macula.boot.starter.rocketmq.test.service.OrderServiceImpl;
import dev.macula.boot.starter.rocketmq.test.vo.OrderVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

/**
 * {@code OrderServiceTest} 事务消息测试
 *
 * @author rain
 * @since 2022/11/30 15:06
 */

@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderServiceImpl orderService;

    @BeforeEach
    public void setGray() {
        GrayVersionMetaHolder.setGrayVersion("feature-001");
    }

    @Test
    public void testSendTxMsg() {
        OrderVo order = new OrderVo();
        order.setOrderNo("M001");
        order.setMainOrderNo("M001");
        order.setTotalAmount(new BigDecimal("100.23"));
        orderService.createOrderWithMq(order);

        orderService.createOrder2(order);

        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
