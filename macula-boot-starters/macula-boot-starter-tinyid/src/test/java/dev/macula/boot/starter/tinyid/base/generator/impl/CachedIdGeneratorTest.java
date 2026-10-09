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
package dev.macula.boot.starter.tinyid.base.generator.impl;

import dev.macula.boot.starter.tinyid.base.entity.SegmentId;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

/**
 * 保证通信替换不改变本地缓存、预加载和分片序列行为。
 * @author Rain
 * @since 6.1.0
 */
class CachedIdGeneratorTest {

    @Test
    void preservesBusinessErrorCodeAndCause() {
        var failure = new dev.macula.boot.exception.BizException("ID503", "发号业务不存在", "safe details");
        assertThatThrownBy(() -> new CachedIdGenerator("unknown", type -> { throw failure; }))
            .isInstanceOfSatisfying(dev.macula.boot.starter.tinyid.base.exception.TinyIdSysException.class, error -> {
                assertThat(error.getCode()).isEqualTo("ID503");
                assertThat(error.getMsg()).isEqualTo("发号业务不存在");
                assertThat(error).hasMessage("safe details").hasCause(failure);
            });
        var local = new dev.macula.boot.starter.tinyid.base.exception.TinyIdSysException("local");
        assertThatThrownBy(() -> new CachedIdGenerator("unknown", type -> { throw local; })).isSameAs(local);
    }

    @Test
    void usesSystemErrorForEmptyResponseAndOtherFailures() {
        var failure = new IllegalStateException("unavailable");
        assertThatThrownBy(() -> new CachedIdGenerator("order", type -> { throw failure; }))
            .isInstanceOfSatisfying(dev.macula.boot.starter.tinyid.base.exception.TinyIdSysException.class, error -> {
                assertThat(error.getCode()).isEqualTo(dev.macula.boot.result.ApiResultCode.SYS_ERROR.getCode());
                assertThat(error).hasCause(failure);
            });
        assertThatThrownBy(() -> new CachedIdGenerator("order", type -> null))
            .isInstanceOfSatisfying(dev.macula.boot.starter.tinyid.base.exception.TinyIdSysException.class, error -> {
                assertThat(error.getCode()).isEqualTo(dev.macula.boot.result.ApiResultCode.SYS_ERROR.getCode());
                assertThat(error).hasNoCause();
            });
    }

    @Test
    void concurrentIdsRemainUniqueAndRespectShard() {
        CachedIdGenerator generator = new CachedIdGenerator("order", type -> segment(0, 9000, 10000, 3, 1));
        try {
            Set<Long> ids = ConcurrentHashMap.newKeySet();
            IntStream.range(0, 1000).parallel().forEach(i -> ids.add(generator.nextId()));
            assertThat(ids).hasSize(1000).allMatch(id -> id % 3 == 1);
        } finally {
            shutdown(generator);
        }
    }

    @Test
    void preloadsAndSwitchesToNextSegment() throws Exception {
        AtomicLong start = new AtomicLong();
        CountDownLatch preloaded = new CountDownLatch(1);
        CachedIdGenerator generator = new CachedIdGenerator("order", type -> {
            long base = start.getAndAdd(100);
            if (base == 100) {
                preloaded.countDown();
            }
            return segment(base, base + 20, base + 100, 1, 0);
        });
        try {
            assertThat(generator.nextId(20)).containsExactlyElementsOf(
                java.util.stream.LongStream.rangeClosed(1, 20).boxed().toList());
            assertThat(preloaded.await(5, TimeUnit.SECONDS)).isTrue();
            ((ExecutorService) ReflectionTestUtils.getField(generator, "executorService"))
                .submit(() -> { }).get(5, TimeUnit.SECONDS);
            assertThat(generator.nextId(81)).containsExactlyElementsOf(
                java.util.stream.LongStream.rangeClosed(21, 101).boxed().toList());
        } finally {
            shutdown(generator);
        }
    }

    private static SegmentId segment(long start, long loading, long max, int delta, int remainder) {
        SegmentId segment = new SegmentId();
        segment.setCurrentId(new AtomicLong(start));
        segment.setLoadingId(loading);
        segment.setMaxId(max);
        segment.setDelta(delta);
        segment.setRemainder(remainder);
        return segment;
    }

    private static void shutdown(CachedIdGenerator generator) {
        ((ExecutorService) ReflectionTestUtils.getField(generator, "executorService")).shutdownNow();
    }
}
