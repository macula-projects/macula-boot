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

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import dev.macula.boot.context.GrayVersionMetaHolder;
import dev.macula.boot.starter.rocketmq.config.GrayRocketMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

/**
 * {@code RocketMQProduceAspect} RocketMQ生产者切面插桩，当GrayVersion不为空时，在消息中带上gray_version标识
 *
 * @author rain
 * @since 2023/9/7 17:32
 */
@Aspect
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class GrayRocketMQProducerAspect {

    private final GrayRocketMQProperties grayRocketMQProperties;

    @Pointcut("execution(* org.apache.rocketmq.client.producer.DefaultMQProducer.send*(..))")
    private void normalProducerPointcut() {
    }

    @Pointcut("execution(* org.apache.rocketmq.client.producer.TransactionMQProducer.send*(..))")
    private void transactionProducerPointcut() {
    }

    @Around("normalProducerPointcut()")
    public Object aroundNormalProducerMessage(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (!this.grayRocketMQProperties.isEnabled())
            return pjp.proceed(args);

        // 先看看是否是灰度请求，再看是否是灰度实例
        String grayVersion = GrayVersionContextHolder.getGrayVersion();
        if (StringUtils.isBlank(grayVersion)) {
            grayVersion = GrayVersionMetaHolder.getGrayVersion();
        }

        if (StringUtils.isBlank(grayVersion)) {
            // 为空表示是基线环境
            return pjp.proceed(args);
        }
        try {
            log.debug("rocketmq producer gray before, args: {}, thread grayVersion: {}", args, grayVersion);
            Message message = null;
            if (args.length >= 1) {
                message = (Message)args[0];
                message.putUserProperty(GlobalConstants.GRAY_VERSION_TAG, grayVersion);
                args[0] = message;
            }
            log.debug("rocketmq producer gray after, args: {}, grayVersion: {}", message, grayVersion);
            return pjp.proceed(args);
        } catch (Exception e) {
            log.error("add grayVersion to rocketmq message error", e);
            return pjp.proceed(args);
        }
    }

    @Around("transactionProducerPointcut()")
    public Object aroundTransactionProducerMessage(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        if (!this.grayRocketMQProperties.isEnabled())
            return pjp.proceed(args);

        // 先看看是否是灰度请求，再看是否是灰度实例
        String grayVersion = GrayVersionContextHolder.getGrayVersion();
        if (StringUtils.isBlank(grayVersion)) {
            grayVersion = GrayVersionMetaHolder.getGrayVersion();
        }

        if (StringUtils.isBlank(grayVersion))
            return pjp.proceed(args);
        try {
            log.debug("rocketmq transaction producer gray before, args: {}, thread grayVersion: {}", args, grayVersion);
            Message message = null;
            if (args.length >= 1) {
                message = (Message)args[0];
                message.putUserProperty(GlobalConstants.GRAY_VERSION_TAG, grayVersion);
                args[0] = message;
            }
            log.debug("rocketmq transaction producer gray after, args: {}, grayVersion: {}", message, grayVersion);
            return pjp.proceed(args);
        } catch (Exception e) {
            log.error("add transaction gray to rocketmq message error", e);
            return pjp.proceed(args);
        }
    }
}
