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
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.EntityManager;
import java.io.Serializable;

/**
 * <p>
 * <b>MaculaJpaRepository</b> 可以获取EntityManager的接口，所有自定义的Repository接口继承该接口
 * </p>
 *
 * @author Rain
 * @version $Id: MaculaJpaRepository.java 3807 2012-11-21 07:31:51Z wilson $
 * @since 2011-4-19
 */
@NoRepositoryBean
public interface MaculaJpaRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {
    /**
     * 获取当前的EntityManager
     *
     * @return 当前JPA的EntityManager
     */
    EntityManager getEntityManager();
}
