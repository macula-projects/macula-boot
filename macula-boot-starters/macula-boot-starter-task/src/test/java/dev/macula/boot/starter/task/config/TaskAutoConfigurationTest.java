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

package dev.macula.boot.starter.task.config;

import com.aizuda.snailjob.client.common.config.SnailJobProperties;
import com.aizuda.snailjob.client.job.core.register.scan.JobExecutorScanner;
import com.aizuda.snailjob.client.starter.SnailJobClientJobCoreAutoConfiguration;
import com.aizuda.snailjob.client.starter.SnailJobClientRetryCoreAutoConfiguration;
import com.aizuda.snailjob.common.core.CommonCoreConfigure;
import dev.macula.boot.starter.task.compat.xxljob.XxlJobClientConflictDetector;
import dev.macula.boot.starter.task.compat.xxljob.XxlJobSnailJobScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests activation and override switches for {@link TaskAutoConfiguration}.
 *
 * @author Rain
 * @since 6.1.0
 */
class TaskAutoConfigurationTest {

    private static final String ENABLED_PROPERTY = "snail-job.enabled";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context -> new TaskEnvironmentPostProcessor()
            .postProcessEnvironment(context.getEnvironment(), null))
        .withConfiguration(AutoConfigurations.of(TaskAutoConfiguration.class,
            CommonCoreConfigure.class, SnailJobClientJobCoreAutoConfiguration.class,
            SnailJobClientRetryCoreAutoConfiguration.class));

    private Object originalEnabled;

    @BeforeEach
    void saveSystemProperty() {
        originalEnabled = System.getProperties().remove(ENABLED_PROPERTY);
    }

    @AfterEach
    void restoreSystemProperty() {
        if (originalEnabled == null) {
            System.getProperties().remove(ENABLED_PROPERTY);
        } else {
            System.getProperties().put(ENABLED_PROPERTY, originalEnabled);
        }
    }

    @Test
    void enablesSnailJobAndCompatibilityAdapterByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TaskProperties.class);
            assertThat(context).hasSingleBean(SnailJobProperties.class);
            assertThat(context).hasSingleBean(JobExecutorScanner.class);
            assertThat(context).hasSingleBean(XxlJobSnailJobScanner.class);
            assertThat(context).hasSingleBean(XxlJobClientConflictDetector.class);
            assertThat(context.getEnvironment().getProperty(ENABLED_PROPERTY, Boolean.class)).isTrue();
        });
    }

    @Test
    void registersEnvironmentDefaultBeforeAutoConfigurationSelection() {
        assertThat(SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class,
            TaskAutoConfigurationTest.class.getClassLoader()))
            .contains(TaskEnvironmentPostProcessor.class.getName());
    }

    @Test
    void taskMasterSwitchDisablesAllTaskConfiguration() {
        contextRunner.withPropertyValues("macula.task.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(TaskProperties.class);
            assertThat(context).doesNotHaveBean(SnailJobProperties.class);
            assertThat(context).doesNotHaveBean(JobExecutorScanner.class);
            assertThat(context).doesNotHaveBean(XxlJobSnailJobScanner.class);
            assertThat(context).doesNotHaveBean(XxlJobClientConflictDetector.class);
        });
    }

    @Test
    void taskMasterSwitchOverridesExplicitSnailJobEnablement() {
        contextRunner.withPropertyValues("macula.task.enabled=false", "snail-job.enabled=true").run(context -> {
            assertThat(context.getEnvironment().getProperty(ENABLED_PROPERTY, Boolean.class)).isFalse();
            assertThat(context).doesNotHaveBean(TaskProperties.class);
            assertThat(context).doesNotHaveBean(SnailJobProperties.class);
            assertThat(context).doesNotHaveBean(JobExecutorScanner.class);
            assertThat(context).doesNotHaveBean(XxlJobSnailJobScanner.class);
            assertThat(context).doesNotHaveBean(XxlJobClientConflictDetector.class);
        });
    }

    @Test
    void explicitSnailJobDisableOverridesAutomaticEnablement() {
        contextRunner.withPropertyValues("snail-job.enabled=false").run(context -> {
            assertThat(context).hasSingleBean(TaskProperties.class);
            assertThat(context).doesNotHaveBean(SnailJobProperties.class);
            assertThat(context).doesNotHaveBean(XxlJobSnailJobScanner.class);
            assertThat(context).hasSingleBean(XxlJobClientConflictDetector.class);
        });
    }

    @Test
    void adapterCanBeDisabledWithoutDisablingNativeSnailJob() {
        contextRunner.withPropertyValues("macula.task.xxl-job-adapter.enabled=false").run(context -> {
            assertThat(context).hasSingleBean(SnailJobProperties.class);
            assertThat(context).hasSingleBean(JobExecutorScanner.class);
            assertThat(context).doesNotHaveBean(XxlJobSnailJobScanner.class);
        });
    }

    @Test
    void userScannerOverridesDefaultAdapterScanner() {
        contextRunner.withUserConfiguration(CustomScannerConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(XxlJobSnailJobScanner.class);
            assertThat(context.getBean(XxlJobSnailJobScanner.class))
                .isSameAs(context.getBean("customXxlJobSnailJobScanner"));
        });
    }

    /**
     * Supplies a user-owned compatibility scanner.
     *
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomScannerConfiguration {

        @Bean
        XxlJobSnailJobScanner customXxlJobSnailJobScanner(ConfigurableApplicationContext context) {
            return new XxlJobSnailJobScanner(context);
        }
    }
}
