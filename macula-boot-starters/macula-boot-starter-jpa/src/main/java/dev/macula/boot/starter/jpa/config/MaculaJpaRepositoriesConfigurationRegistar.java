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

package dev.macula.boot.starter.jpa.config;

import com.blinkfox.fenix.jpa.FenixJpaRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Locale;

/**
 * <p>
 * <b>MaculaJpaRepositoriesConfigurationRegistar</b> Spring Data JPA配置注册
 * </p>
 *
 * @author Rain
 * @since 2019-02-18
 */
class MaculaJpaRepositoriesConfigurationRegistar extends AbstractRepositoryConfigurationSourceSupport {

    private BootstrapMode bootstrapMode = null;

    protected Class<? extends Annotation> getAnnotation() {
        return EnableJpaRepositories.class;
    }

    protected Class<?> getConfiguration() {
        return MaculaJpaRepositoriesConfigurationRegistar.EnableJpaRepositoriesConfiguration.class;
    }

    protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
        return new JpaRepositoryConfigExtension();
    }

    protected BootstrapMode getBootstrapMode() {
        return this.bootstrapMode == null ? BootstrapMode.DEFAULT : this.bootstrapMode;
    }

    public void setEnvironment(Environment environment) {
        super.setEnvironment(environment);
        this.configureBootstrapMode(environment);
    }

    private void configureBootstrapMode(Environment environment) {
        String property = environment.getProperty("spring.data.jpa.repositories.bootstrap-mode");
        if (StringUtils.hasText(property)) {
            this.bootstrapMode = BootstrapMode.valueOf(property.toUpperCase(Locale.ENGLISH));
        }
    }

    @EnableJpaRepositories(repositoryFactoryBeanClass = FenixJpaRepositoryFactoryBean.class)
    private static class EnableJpaRepositoriesConfiguration {
        private EnableJpaRepositoriesConfiguration() {
        }
    }
}
