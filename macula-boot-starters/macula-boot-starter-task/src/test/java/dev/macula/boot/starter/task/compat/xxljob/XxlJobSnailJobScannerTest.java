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

package dev.macula.boot.starter.task.compat.xxljob;

import com.aizuda.snailjob.client.common.config.SnailJobProperties;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.cache.JobExecutorInfoCache;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.job.core.dto.JobExecutorInfo;
import com.aizuda.snailjob.client.job.core.register.JobExecutorRegistrar;
import com.aizuda.snailjob.client.job.core.register.scan.JobExecutorScanner;
import com.aizuda.snailjob.common.core.context.SnailSpringContext;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests discovery, validation and lifecycle behavior of {@link XxlJobSnailJobScanner}.
 *
 * @author Rain
 * @since 6.1.0
 */
class XxlJobSnailJobScannerTest {

    @Test
    void discoversHandlerAndRunsLifecycle() {
        LifecycleHandler handler = new LifecycleHandler();
        try (GenericApplicationContext context = contextWith("handler", LifecycleHandler.class, handler)) {
            XxlJobSnailJobScanner scanner = new XxlJobSnailJobScanner(context);

            List<JobExecutorInfo> infos = scanner.doScan();

            assertThat(infos).extracting(JobExecutorInfo::getExecutorName).containsExactly("legacyHandler");
            assertThat(handler.events).containsExactly("init");
            scanner.destroy();
            assertThat(handler.events).containsExactly("init", "destroy");
        }
    }

    @Test
    void invokesHandlerBehindJdkProxy() throws Exception {
        ProxyTarget target = new ProxyTarget();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setInterfaces(HandlerContract.class);
        HandlerContract proxy = (HandlerContract) proxyFactory.getProxy();
        try (GenericApplicationContext context = contextWith("handler", HandlerContract.class, proxy)) {
            XxlJobSnailJobScanner scanner = new XxlJobSnailJobScanner(context);
            JobExecutorInfo info = scanner.doScan().get(0);

            info.getMethod().invoke(info.getExecutor(), new JobArgs());

            assertThat(target.invoked).isTrue();
        }
    }

    @Test
    void rejectsBlankDuplicateAndPrimitiveHandlers() {
        assertScanFails(new BlankHandler(), BlankHandler.class, "must not be blank");
        assertScanFails(new DuplicateHandler(), DuplicateHandler.class, "Duplicate SnailJob executor name");
        assertScanFails(new PrimitiveHandler(), PrimitiveHandler.class, "must not be primitive");
    }

    @Test
    void rejectsNameSharedWithNativeSnailJobHandler() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("legacy", LegacyCollisionHandler.class, LegacyCollisionHandler::new);
        context.registerBean("native", NativeCollisionHandler.class, NativeCollisionHandler::new);
        context.refresh();
        try (context) {
            XxlJobSnailJobScanner scanner = new XxlJobSnailJobScanner(context);

            assertThatThrownBy(scanner::doScan)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate SnailJob executor name: sharedHandler");
        }
    }

    @Test
    void registersNativeAndLegacyHandlersInOfficialSnailJobCache() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(SnailSpringContext.class);
        context.registerBean(SnailJobProperties.class);
        context.registerBean("legacy", RegisteredLegacyHandler.class, RegisteredLegacyHandler::new);
        context.registerBean("native", RegisteredNativeHandler.class, RegisteredNativeHandler::new);
        context.refresh();
        try (context) {
            JobExecutorScanner nativeScanner = new JobExecutorScanner();
            nativeScanner.setApplicationContext(context);
            XxlJobSnailJobScanner legacyScanner = new XxlJobSnailJobScanner(context);
            JobExecutorRegistrar registrar = new JobExecutorRegistrar(List.of(nativeScanner, legacyScanner));

            registrar.registerRetryHandler(nativeScanner.doScan());
            registrar.registerRetryHandler(legacyScanner.doScan());

            assertThat(JobExecutorInfoCache.isExisted("contractNativeHandler")).isTrue();
            assertThat(JobExecutorInfoCache.isExisted("contractLegacyHandler")).isTrue();
        }
    }

    @Test
    void destroysInitializedHandlersInReverseOrderWhenLaterInitializationFails() {
        List<String> events = new ArrayList<>();
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("first", FirstLifecycleHandler.class, () -> new FirstLifecycleHandler(events));
        context.registerBean("second", FailingLifecycleHandler.class, () -> new FailingLifecycleHandler(events));
        context.refresh();
        try (context) {
            XxlJobSnailJobScanner scanner = new XxlJobSnailJobScanner(context);

            assertThatThrownBy(scanner::doScan)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("initialize");
            assertThat(events).containsExactly("first-init", "second-init", "first-destroy");
        }
    }

    private <T> GenericApplicationContext contextWith(String name, Class<T> type, T bean) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(name, type, () -> bean);
        context.refresh();
        return context;
    }

    private <T> void assertScanFails(T bean, Class<T> type, String message) {
        try (GenericApplicationContext context = contextWith("handler", type, bean)) {
            XxlJobSnailJobScanner scanner = new XxlJobSnailJobScanner(context);
            assertThatThrownBy(scanner::doScan)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(message);
        }
    }

    /**
     * Records lifecycle callbacks for one legacy handler.
     *
     * @since 6.1.0
     */
    static class LifecycleHandler {

        private final List<String> events = new ArrayList<>();

        @XxlJob(value = "legacyHandler", init = "init", destroy = "destroy")
        public void execute() {
        }

        public void init() {
            events.add("init");
        }

        public void destroy() {
            events.add("destroy");
        }
    }

    /**
     * Contract used to build a JDK proxy.
     *
     * @since 6.1.0
     */
    interface HandlerContract {

        void execute();
    }

    /**
     * Legacy handler behind a JDK proxy.
     *
     * @since 6.1.0
     */
    static class ProxyTarget implements HandlerContract {

        private boolean invoked;

        @Override
        @XxlJob("proxiedHandler")
        public void execute() {
            invoked = true;
        }
    }

    /**
     * Invalid blank-name handler.
     *
     * @since 6.1.0
     */
    static class BlankHandler {

        @XxlJob(" ")
        void execute() {
        }
    }

    /**
     * Invalid duplicate-name handler.
     *
     * @since 6.1.0
     */
    static class DuplicateHandler {

        @XxlJob("duplicate")
        void first() {
        }

        @XxlJob("duplicate")
        void second() {
        }
    }

    /**
     * Invalid primitive-parameter handler.
     *
     * @since 6.1.0
     */
    static class PrimitiveHandler {

        @XxlJob("primitive")
        void execute(int value) {
        }
    }

    /**
     * Legacy side of a cross-framework name collision.
     *
     * @since 6.1.0
     */
    static class LegacyCollisionHandler {

        @XxlJob("sharedHandler")
        void execute() {
        }
    }

    /**
     * Native SnailJob side of a cross-framework name collision.
     *
     * @since 6.1.0
     */
    static class NativeCollisionHandler {

        @JobExecutor(name = "sharedHandler")
        public void execute(JobArgs args) {
        }
    }

    /**
     * Legacy handler used by the official registrar contract test.
     *
     * @since 6.1.0
     */
    static class RegisteredLegacyHandler {

        @XxlJob("contractLegacyHandler")
        void execute() {
        }
    }

    /**
     * Native handler used by the official registrar contract test.
     *
     * @since 6.1.0
     */
    static class RegisteredNativeHandler {

        @JobExecutor(name = "contractNativeHandler")
        void execute(JobArgs args) {
        }
    }

    /**
     * Successfully initialized handler used to verify rollback order.
     *
     * @since 6.1.0
     */
    static class FirstLifecycleHandler {

        private final List<String> events;

        FirstLifecycleHandler(List<String> events) {
            this.events = events;
        }

        @XxlJob(value = "firstLifecycle", init = "init", destroy = "destroy")
        void execute() {
        }

        void init() {
            events.add("first-init");
        }

        void destroy() {
            events.add("first-destroy");
        }
    }

    /**
     * Handler whose initialization fails after an earlier handler succeeds.
     *
     * @since 6.1.0
     */
    static class FailingLifecycleHandler {

        private final List<String> events;

        FailingLifecycleHandler(List<String> events) {
            this.events = events;
        }

        @XxlJob(value = "failingLifecycle", init = "init")
        void execute() {
        }

        void init() {
            events.add("second-init");
            throw new IllegalStateException("expected init failure");
        }
    }
}
