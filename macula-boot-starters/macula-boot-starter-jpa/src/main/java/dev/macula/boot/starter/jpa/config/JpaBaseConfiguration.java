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

import dev.macula.boot.starter.jpa.domain.support.AuditorAwareStub;
import dev.macula.boot.starter.jpa.domain.support.DbDateTimeProvider;
import dev.macula.boot.starter.jpa.hibernate.audit.AuditedEventListener;
import dev.macula.boot.starter.jpa.jpa.templatequery.template.FreemarkerSqlTemplates;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JPA配置的基类
 *
 * @author Rain
 */

@EnableJpaAuditing(auditorAwareRef = "auditorAwareStub", dateTimeProviderRef = "dbDateTimeProvider")
@EnableConfigurationProperties(HibernateProperties.class)
public abstract class JpaBaseConfiguration {

    @Autowired(required = false)
    ObjectProvider<PersistenceUnitManager> persistenceUnitManager;
    private DataSource dataSource;
    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    private HibernateProperties hibernateProperties;

    protected JpaBaseConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected Map<String, Object> getVendorProperties() {
        HibernateSettings settings = new HibernateSettings();

        List<HibernatePropertiesCustomizer> customizers = new ArrayList<>();
        customizers.add((Map<String, Object> hibernateProperties) -> {
            hibernateProperties.put("hibernate.ejb.event.post-update", AuditedEventListener.class.getName());
            hibernateProperties.put("hibernate.ejb.event.post-delete", AuditedEventListener.class.getName());
        });

        settings.hibernatePropertiesCustomizers(customizers);

        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), settings);
    }

    protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder() {
        return new EntityManagerFactoryBuilder(getJpaVendorAdapter(), jpaProperties.getProperties(),
            persistenceUnitManager.getIfAvailable());
    }

    protected JpaVendorAdapter getJpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(this.jpaProperties.isShowSql());
        // TODO 待验证是否可以不用设置，让JPA自己确认
        // adapter.setDatabase(this.jpaProperties.determineDatabase(this.dataSource));
        adapter.setDatabasePlatform(this.jpaProperties.getDatabasePlatform());
        adapter.setGenerateDdl(this.jpaProperties.isGenerateDdl());
        return adapter;
    }

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
    @ConditionalOnMissingBean
    public FreemarkerSqlTemplates freemarkerSqlTemplates() {
        return new FreemarkerSqlTemplates();
    }
}
