/*
 * Copyright (c) 2026 Macula
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
package dev.macula.boot.starter.binlog4j.test;

import dev.macula.boot.starter.binlog4j.position.BinlogPosition;
import dev.macula.boot.starter.binlog4j.position.RedisBinlogPositionHandler;
import dev.macula.boot.starter.binlog4j.utils.CacheConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 Redis 历史位点 JSON 和 Jackson 3 读写兼容性。
 *
 * @author Rain
 * @since 6.1.0
 */
class BinlogPositionJsonTest {

    private final RedissonClient client = mock(RedissonClient.class);
    @SuppressWarnings("unchecked")
    private final RBucket<Object> bucket = mock(RBucket.class);
    private final RedisBinlogPositionHandler handler = new RedisBinlogPositionHandler(client);

    @Test
    void readsLegacyFastjsonPosition() {
        when(client.getBucket(CacheConstants.CACHE_BINLOG_PREFIX + "7")).thenReturn(bucket);
        when(bucket.get()).thenReturn("""
                {"filename":"mysql-bin.000012","gtidSet":"uuid:1-99","position":9007199254740993,"serverId":7}
                """);
        BinlogPosition result = handler.loadPosition(7L);
        assertThat(result.getServerId()).isEqualTo(7L);
        assertThat(result.getPosition()).isEqualTo(9007199254740993L);
        assertThat(result.getFilename()).isEqualTo("mysql-bin.000012");
        assertThat(result.getGtidSet()).isEqualTo("uuid:1-99");
    }

    @Test
    void roundTripsWithoutChangingPersistedFieldNames() {
        when(client.getBucket(CacheConstants.CACHE_BINLOG_PREFIX + "7")).thenReturn(bucket);
        BinlogPosition position = new BinlogPosition();
        position.setServerId(7L);
        position.setPosition(9007199254740993L);
        position.setFilename("mysql-bin.000012");
        position.setGtidSet("uuid:1-99");
        handler.savePosition(position);
        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class);
        verify(bucket).set(saved.capture());
        assertThat(saved.getValue()).isInstanceOf(String.class);
        JsonNode json = JsonMapper.builder().build().readTree((String) saved.getValue());
        assertThat(json.has("serverId")).isTrue();
        assertThat(json.has("gtidSet")).isTrue();
        assertThat(json.has("server_id")).isFalse();
        assertThat(json.has("gtid_set")).isFalse();
        when(bucket.get()).thenReturn(saved.getValue());
        assertThat(handler.loadPosition(7L)).usingRecursiveComparison().isEqualTo(position);
    }

    @Test
    void supportsMissingAndNullOptionalFields() {
        when(client.getBucket(CacheConstants.CACHE_BINLOG_PREFIX + "7")).thenReturn(bucket);
        when(bucket.get()).thenReturn("{\"serverId\":7,\"position\":4,\"gtidSet\":null}");
        BinlogPosition result = handler.loadPosition(7L);
        assertThat(result.getGtidSet()).isNull();
        assertThat(result.getFilename()).isNull();
        handler.savePosition(result);
        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class);
        verify(bucket).set(saved.capture());
        when(bucket.get()).thenReturn(saved.getValue());
        assertThat(handler.loadPosition(7L)).usingRecursiveComparison().isEqualTo(result);
    }

    @Test
    void returnsNullWhenPositionIsAbsent() {
        when(client.getBucket(CacheConstants.CACHE_BINLOG_PREFIX + "7")).thenReturn(bucket);
        assertThat(handler.loadPosition(7L)).isNull();
    }
}
