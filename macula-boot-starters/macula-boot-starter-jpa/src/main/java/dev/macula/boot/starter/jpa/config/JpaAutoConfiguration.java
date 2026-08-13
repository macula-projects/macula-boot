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

/*
 * Copyright 2004-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.starter.jpa.config;

import dev.macula.boot.starter.jpa.entity.support.AuditorAwareStub;
import dev.macula.boot.starter.jpa.entity.support.DbDateTimeProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

import javax.sql.DataSource;
import java.util.Map;

/**
 * JPA配置的基类
 *
 * @author Rain
 * @since 5.0.0
 */

@AutoConfiguration(before = JpaRepositoriesAutoConfiguration.class,
    after = {HibernateJpaAutoConfiguration.class, TaskExecutionAutoConfiguration.class})
@ConditionalOnMissingBean({JpaRepositoryFactoryBean.class, JpaRepositoryConfigExtension.class})
@ConditionalOnBean({DataSource.class})
@ConditionalOnClass({JpaRepository.class})
@ConditionalOnProperty(prefix = "spring.data.jpa.repositories", name = {"enabled"}, havingValue = "true",
    matchIfMissing = true)
@EnableJpaAuditing(auditorAwareRef = "auditorAwareStub", dateTimeProviderRef = "dbDateTimeProvider")
@Import(MaculaJpaRepositoriesConfigurationRegistar.class)
public class JpaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AuditorAwareStub auditorAwareStub() {
        return new AuditorAwareStub();
    }

    @Bean
    @ConditionalOnMissingBean
    public DbDateTimeProvider dbDateTimeProvider() {
        return new DbDateTimeProvider();
    }

    @Bean
    @Conditional(JpaAutoConfiguration.BootstrapExecutorCondition.class)
    public EntityManagerFactoryBuilderCustomizer entityManagerFactoryBootstrapExecutorCustomizer(
        Map<String, AsyncTaskExecutor> taskExecutors) {
        return (builder) -> {
            AsyncTaskExecutor bootstrapExecutor = determineBootstrapExecutor(taskExecutors);
            if (bootstrapExecutor != null) {
                builder.setBootstrapExecutor(bootstrapExecutor);
            }
        };
    }

    private AsyncTaskExecutor determineBootstrapExecutor(Map<String, AsyncTaskExecutor> taskExecutors) {
        if (taskExecutors.size() == 1) {
            return taskExecutors.values().iterator().next();
        }
        return taskExecutors.get(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
    }

    /**
     * 判断 JPA Repository 是否需要异步引导执行器。
     *
     * @since 5.0.0
     */
    private static final class BootstrapExecutorCondition extends AnyNestedCondition {

        BootstrapExecutorCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        /**
         * 延迟引导模式匹配条件。
         *
         * @since 5.0.0
         */
        @ConditionalOnProperty(prefix = "spring.data.jpa.repositories", name = "bootstrap-mode",
            havingValue = "deferred")
        static class DeferredBootstrapMode {

        }

        /**
         * 懒加载引导模式匹配条件。
         *
         * @since 5.0.0
         */
        @ConditionalOnProperty(prefix = "spring.data.jpa.repositories", name = "bootstrap-mode", havingValue = "lazy")
        static class LazyBootstrapMode {

        }
    }
}
