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
package dev.macula.boot.starter.jpa.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import java.io.Serializable;

/**
 * <p>MaculaJpaRepositoryFactoryBean</p>扩展自{@link JpaRepositoryFactoryBean}
 *
 * @author Rain
 * @version $Id: MaculaJpaRepositoryFactoryBean.java 3818 2012-11-23 01:50:20Z wilson $
 * @since 2011-2-15
 */
public class MaculaJpaRepositoryFactoryBean<T extends JpaRepository<Object, Serializable>>
    extends JpaRepositoryFactoryBean<T, Object, Serializable> {

    /**
     * @param repositoryInterface
     */
    public MaculaJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {
        return new MaculaJpaRepositoryFactory(em);
    }
}
