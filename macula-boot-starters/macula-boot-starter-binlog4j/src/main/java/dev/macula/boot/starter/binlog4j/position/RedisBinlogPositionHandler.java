/*
 * Copyright (c) 2023-2026 Macula
 * macula.dev, China
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

package dev.macula.boot.starter.binlog4j.position;

import com.alibaba.fastjson2.JSON;
import dev.macula.boot.starter.binlog4j.utils.CacheConstants;
import org.redisson.api.RedissonClient;

/**
 * 基于 Redis 保存和恢复 Binlog 消费位点。
 *
 * @author rain
 * @since 5.0.0
 */
public class RedisBinlogPositionHandler implements BinlogPositionHandler {

    private final RedissonClient redissonClient;

    public RedisBinlogPositionHandler(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public BinlogPosition loadPosition(Long serverId) {
        Object value = redissonClient.getBucket(getKey(serverId)).get();
        if (value != null) {
            return JSON.parseObject(value.toString(), BinlogPosition.class);
        }
        return null;
    }

    @Override
    public void savePosition(BinlogPosition position) {
        redissonClient.getBucket(getKey(position.getServerId())).set(JSON.toJSONString(position));
    }

    private String getKey(Long serverId) {
        return CacheConstants.CACHE_BINLOG_PREFIX + serverId.toString();
    }
}
