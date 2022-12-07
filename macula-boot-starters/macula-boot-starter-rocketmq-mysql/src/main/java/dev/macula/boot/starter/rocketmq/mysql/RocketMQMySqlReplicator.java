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

package dev.macula.boot.starter.rocketmq.mysql;

import dev.macula.boot.starter.rocketmq.mysql.config.RocketMQMysqlProperties;
import org.apache.rocketmq.mysql.Config;
import org.apache.rocketmq.mysql.Replicator;
import org.apache.rocketmq.mysql.binlog.EventProcessor;
import org.apache.rocketmq.mysql.binlog.Transaction;
import org.apache.rocketmq.mysql.position.BinlogPosition;
import org.apache.rocketmq.mysql.position.BinlogPositionLogThread;
import org.apache.rocketmq.mysql.mq.RocketMQMySqlProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code RocketMQMySqlReplicator} 基于mysql binlog将日志转为RocketMQ消息发送出去，
 * 消费方可以订阅消息用于缓存更新，业务异步处理等
 *
 * @author rain
 * @since 2022/12/5 14:13
 */
public class RocketMQMySqlReplicator implements Replicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Replicator.class);

    private static final Logger POSITION_LOGGER = LoggerFactory.getLogger("PositionLogger");

    private Config config;

    private RocketMQMySqlProducer rocketMQProducer;

    private EventProcessor eventProcessor;

    private Object lock = new Object();
    private BinlogPosition nextBinlogPosition;
    private long nextQueueOffset;
    private long xid;

    public RocketMQMySqlReplicator(RocketMQMysqlProperties properties) {
        try {
            config = new Config();
            // TODO 初始化config

            rocketMQProducer = new RocketMQMySqlProducer(config);
            rocketMQProducer.start();

            BinlogPositionLogThread binlogPositionLogThread = new BinlogPositionLogThread(this);
            binlogPositionLogThread.start();

            eventProcessor = new EventProcessor(this);
            eventProcessor.start();

        } catch (Exception e) {
            LOGGER.error("Start error.", e);
            System.exit(1);
        }
    }

    public void commit(Transaction transaction, boolean isComplete) {

        String json = transaction.toJson();

        for (int i = 0; i < 3; i++) {
            try {
                if (isComplete) {
                    long offset = rocketMQProducer.push(json);
                    synchronized (lock) {
                        xid = transaction.getXid();
                        nextBinlogPosition = transaction.getNextBinlogPosition();
                        nextQueueOffset = offset;
                    }

                } else {
                    rocketMQProducer.push(json);
                }
                break;

            } catch (Exception e) {
                LOGGER.error("Push error,retry:" + (i + 1) + ",", e);
            }
        }
    }

    public void logPosition() {

        String binlogFilename = null;
        long xid = 0L;
        long nextPosition = 0L;
        long nextOffset = 0L;

        synchronized (lock) {
            if (nextBinlogPosition != null) {
                xid = this.xid;
                binlogFilename = nextBinlogPosition.getBinlogFilename();
                nextPosition = nextBinlogPosition.getPosition();
                nextOffset = nextQueueOffset;
            }
        }

        if (binlogFilename != null) {
            POSITION_LOGGER.info("XID: {},   BINLOG_FILE: {},   NEXT_POSITION: {},   NEXT_OFFSET: {}",
                    xid, binlogFilename, nextPosition, nextOffset);
        }

    }

    public Config getConfig() {
        return config;
    }

    public BinlogPosition getNextBinlogPosition() {
        return nextBinlogPosition;
    }
}
