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
package dev.macula.boot.starter.cloud.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.context.GrayVersionContextHolder;
import dev.macula.boot.context.TenantContextHolder;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Reactor 自动上下文传播对 Macula ThreadLocalAccessor 的兼容测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class ReactiveContextPropagationTest {

    @AfterEach
    void cleanup() {
        Hooks.disableAutomaticContextPropagation();
        TenantContextHolder.clearCurrentTenantId();
        GrayVersionContextHolder.clear();
    }

    @Test
    void shouldRestoreObservationTenantAndGrayVersionAcrossSchedulerSwitch() {
        Hooks.enableAutomaticContextPropagation();
        TenantContextHolder.setCurrentTenantId(1001L);
        GrayVersionContextHolder.setGrayVersion("gray-v2");
        ObservationRegistry registry = ObservationRegistry.create();
        Observation observation = Observation.start("reactive-test", registry);
        Scheduler scheduler = Schedulers.newSingle("context-propagation-test");
        try (Observation.Scope ignored = observation.openScope()) {
            String observed = Mono.defer(() -> Mono.just(TenantContextHolder.getCurrentTenantId()
                    + ":" + GrayVersionContextHolder.getGrayVersion() + ":"
                    + (registry.getCurrentObservation() == observation)))
                .subscribeOn(scheduler)
                .publishOn(Schedulers.parallel())
                .block();

            assertThat(observed).isEqualTo("1001:gray-v2:true");
        } finally {
            observation.stop();
            scheduler.dispose();
        }
    }

    @Test
    void shouldNotRestoreThreadLocalsWhenAutomaticPropagationIsDisabled() {
        TenantContextHolder.setCurrentTenantId(1001L);
        GrayVersionContextHolder.setGrayVersion("gray-v2");
        ObservationRegistry registry = ObservationRegistry.create();
        Observation observation = Observation.start("reactive-disabled-test", registry);
        Scheduler scheduler = Schedulers.newSingle("context-propagation-disabled-test");
        try (Observation.Scope ignored = observation.openScope()) {
            String observed = Mono.fromCallable(() -> String.valueOf(TenantContextHolder.getCurrentTenantId())
                    + ":" + String.valueOf(GrayVersionContextHolder.getGrayVersion()) + ":"
                    + (registry.getCurrentObservation() != null))
                .subscribeOn(scheduler)
                .block();

            assertThat(observed).isEqualTo("null:null:false");
        } finally {
            observation.stop();
            scheduler.dispose();
        }
    }
}
