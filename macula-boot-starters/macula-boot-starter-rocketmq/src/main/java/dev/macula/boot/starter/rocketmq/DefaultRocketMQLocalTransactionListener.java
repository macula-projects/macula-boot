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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import dev.macula.boot.starter.rocketmq.annotation.TxMqCheck;
import dev.macula.boot.starter.rocketmq.annotation.TxMqExecute;
import dev.macula.boot.starter.rocketmq.config.Constants;
import lombok.SneakyThrows;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code DefaultRocketMQLocalTransactionListener} 默认事务消息本地监听器，调用RocketMQTemplate.sendMessageInTransaction
 * 方法的时候，会调用该类的executeLocalTransaction方法。如果失败，会调用checkLocalTransaction以确定事务消息是否提交
 * <p>
 * <p>
 * <p>
 * {@code // 发送半事务消息 public void createOrderWithMq(OrderVo order) { TxMqMessage txMsg = new TxMqMessage(order,
 * this.getClass(), BIZ_NAME_ORDER, order.getOrderNo()); rocketMQTemplate.sendMessageInTransaction(TOPIC_ORDER, txMsg,
 * new Object[] { order }); } }
 *
 * @author rain
 * @since 2022/11/28 22:55
 */

@RocketMQTransactionListener
public class DefaultRocketMQLocalTransactionListener
    implements RocketMQLocalTransactionListener, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final Map<String, Object> cacheMap = new HashMap<>();

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            String className = (String)msg.getHeaders().get(Constants.BEAN_CLASS_NAME);
            String bizName = (String)msg.getHeaders().get(Constants.BIZ_NAME);

            // 执行业务方法
            if (ArrayUtil.isArray(arg)) {
                ReflectUtil.invoke(getBean(className), findTxMqExecute(className, bizName), (Object[])arg);
            } else {
                ReflectUtil.invoke(getBean(className), findTxMqExecute(className, bizName), arg);
            }

            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        try {
            String className = (String)msg.getHeaders().get(Constants.BEAN_CLASS_NAME);
            String bizName = (String)msg.getHeaders().get(Constants.BIZ_NAME);
            String checkId = (String)msg.getHeaders().get(Constants.CHECK_ID);

            // 执行检查方法
            Boolean ret = ReflectUtil.invoke(getBean(className), findTxMqCheck(className, bizName), checkId);

            if (ret) {
                return RocketMQLocalTransactionState.COMMIT;
            }

            return RocketMQLocalTransactionState.UNKNOWN;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    @SneakyThrows
    private Object getBean(String className) {
        if (!cacheMap.containsKey(className)) {
            cacheMap.put(className, applicationContext.getBean(Class.forName(className)));
        }
        return cacheMap.get(className);
    }

    @SneakyThrows
    private Method findTxMqExecute(String className, String bizName) {
        Method method = (Method)cacheMap.get(className + bizName + "_execute");
        if (method == null) {
            Class<?> cls = Class.forName(className);
            Method[] methods = cls.getMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(TxMqExecute.class) && bizName.equals(
                    m.getAnnotation(TxMqExecute.class).value())) {
                    cacheMap.put(className + bizName + "_execute", m);
                    method = m;
                    break;
                }
            }
        }
        return method;
    }

    @SneakyThrows
    private Method findTxMqCheck(String className, String bizName) {
        Method method = (Method)cacheMap.get(className + bizName + "_check");
        if (method == null) {
            Class<?> cls = Class.forName(className);
            Method[] methods = cls.getMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(TxMqCheck.class) && bizName.equals(
                    m.getAnnotation(TxMqCheck.class).value())) {
                    cacheMap.put(className + bizName + "_check", m);
                    method = m;
                    break;
                }
            }
        }
        return method;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
