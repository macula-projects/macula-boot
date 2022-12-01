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

package dev.macula.boot.starter.rocketmq.config;

/**
 * {@code Constants} RocketMQ模块的常量类
 *
 * @author rain
 * @since 2022/11/29 18:06
 */
public class Constants {

    /**
     * 事务消息监听-所属类
     */
    public final static String BEAN_CLASS_NAME = "BEAN_CLASS_NAME";

    /**
     * 业务消息ID 例如订单号，支付流水号
     */
    public final static String CHECK_ID = "CHECK_ID";

    /**
     * 业务名称，为了区分不同的@Tx注解
     */
    public static final String BIZ_NAME = "BIZ_NAME";
}
