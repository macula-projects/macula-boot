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

package dev.macula.boot.starter.binlog4j;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import dev.macula.boot.starter.binlog4j.dispatcher.BinlogEventDispatcher;
import dev.macula.boot.starter.binlog4j.enums.BinlogClientMode;
import dev.macula.boot.starter.binlog4j.position.BinlogPosition;
import dev.macula.boot.starter.binlog4j.position.BinlogPositionHandler;
import dev.macula.boot.starter.binlog4j.position.RedisBinlogPositionHandler;
import dev.macula.boot.starter.binlog4j.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BinlogClient implements IBinlogClient {

    private final BinlogClientConfig clientConfig;

    private BinaryLogClient client;

    private BinlogPositionHandler positionHandler;

    private RedissonClient redissonClient;

    private final List<BinlogEventHandlerDetails> eventHandlerMap = new ArrayList<>();

    private final ExecutorService executor;

    public BinlogClient(BinlogClientConfig clientConfig, RedissonClient redissonClient) {
        if (clientConfig.isPersistence() || clientConfig.getMode() == BinlogClientMode.cluster) {
            if (redissonClient == null) {
                throw new RuntimeException("Cluster mode or persistence enabled, missing Redis configuration");
            }
            this.redissonClient = redissonClient;
            this.positionHandler = new RedisBinlogPositionHandler(redissonClient);
        }
        this.clientConfig = clientConfig;
        this.executor = Executors.newCachedThreadPool();
    }

    public void registerEventHandler(String database, String table, IBinlogEventHandler<?> eventHandler) {
        BinlogEventHandlerDetails eventHandlerDetails = new BinlogEventHandlerDetails();
        eventHandlerDetails.setDatabase(database);
        eventHandlerDetails.setTable(table);
        eventHandlerDetails.setClientConfig(clientConfig);
        eventHandlerDetails.setEntityClass(ClassUtils.getGenericType(eventHandler.getClass()));
        eventHandlerDetails.setEventHandler(eventHandler);
        this.eventHandlerMap.add(eventHandlerDetails);
    }

    @Override
    public void connect() {
        BinlogClientMode clientMode = clientConfig.getMode();
        if (clientMode == BinlogClientMode.cluster) {
            ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
            scheduledExecutor.scheduleWithFixedDelay(this::runWithCluster, 1, 1000, TimeUnit.MILLISECONDS);
        } else {
            executor.submit(this::runWithStandalone);
        }
    }

    @Override
    public void disconnect() {
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception e) {
                log.error("disconnect error", e);
            }
        }
    }

    public void runWithStandalone() {
        try {
            client = new BinaryLogClient(clientConfig.getHost(), clientConfig.getPort(), clientConfig.getUsername(), clientConfig.getPassword());
            client.registerEventListener(new BinlogEventDispatcher(this.clientConfig, this.client, positionHandler, this.eventHandlerMap));
            client.setKeepAlive(clientConfig.isKeepAlive());
            client.setKeepAliveInterval(clientConfig.getKeepAliveInterval());
            client.setHeartbeatInterval(clientConfig.getHeartbeatInterval());
            client.setConnectTimeout(clientConfig.getConnectTimeout());
            client.setServerId(clientConfig.getServerId());
            if (clientConfig.isPersistence()) {
                if (!clientConfig.isInaugural()) {
                    if (positionHandler != null) {
                        // 恢复位置现场
                        BinlogPosition binlogPosition = positionHandler.loadPosition(clientConfig.getServerId());
                        if (binlogPosition != null) {
                            if (clientConfig.isGtidMode()) {
                                client.setGtidSetFallbackToPurged(clientConfig.isGtidPurged());
                                client.setGtidSet(binlogPosition.getGtidSet() == null ? getDefaultGtidSet() : binlogPosition.getGtidSet());
                            } else {
                                client.setBinlogFilename(binlogPosition.getFilename());
                                client.setBinlogPosition(binlogPosition.getPosition());
                            }
                        } else {
                            if (clientConfig.isGtidMode()) {
                                client.setGtidSetFallbackToPurged(clientConfig.isGtidPurged());
                                client.setGtidSet(getDefaultGtidSet());
                            }
                        }
                    } else {
                        if (clientConfig.isGtidMode()) {
                            client.setGtidSetFallbackToPurged(clientConfig.isGtidPurged());
                            client.setGtidSet(getDefaultGtidSet());
                        }
                    }
                }
            }
            client.connect();
        } catch (Exception e) {
            log.error("run binlog error", e);
        }
    }

    public void runWithCluster() {
        RLock lock = redissonClient.getLock(clientConfig.getKey());
        try {
            if (lock.tryLock()) {
                runWithStandalone();
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String getDefaultGtidSet() {
        String gtidSet =  clientConfig.getGtidSetDefault();
        return gtidSet == null ? "" : gtidSet;
    }
}
