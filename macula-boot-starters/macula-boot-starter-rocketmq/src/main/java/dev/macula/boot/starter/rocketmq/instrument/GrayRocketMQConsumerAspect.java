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

package dev.macula.boot.starter.rocketmq.instrument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

/**
 * {@code GrayRocketMQConsumerAspect}  RocketMQ消费者切面插桩，根据灰度标识过滤消息
 *
 * @author rain
 * @since 2023/12/11 10:56
 */
@Aspect
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class GrayRocketMQConsumerAspect {

    private final GrayFilterMessageHookImpl grayFilterMessageHook;

    @Pointcut("execution(* org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer.start*(..))")
    private void listenerContainerStart() {
    }

    @Around("listenerContainerStart()")
    public Object aroundListenerContainerStart(ProceedingJoinPoint pjp) throws Throwable {
        Object originalObject = pjp.getTarget();
        if (originalObject instanceof DefaultRocketMQListenerContainer) {
            DefaultRocketMQListenerContainer defaultRocketMQListenerContainer =
                (DefaultRocketMQListenerContainer)originalObject;
            defaultRocketMQListenerContainer.getConsumer().getDefaultMQPushConsumerImpl()
                .registerFilterMessageHook(this.grayFilterMessageHook);
        } else {
            log.error("rocketmq consumer lane before, args: {}, thread laneId: {}", pjp.getArgs(),
                "not DefaultRocketMQListenerContainer");
        }
        Object[] args = pjp.getArgs();
        return pjp.proceed(args);
    }
}
