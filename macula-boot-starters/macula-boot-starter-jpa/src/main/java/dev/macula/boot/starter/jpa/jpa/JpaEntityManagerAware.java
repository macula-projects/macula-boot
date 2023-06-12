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

import javax.persistence.EntityManager;

/**
 * <p>
 * <b>JpaEntityManagerAware</b> 是Spring Data JPA的Repository接口自定义接口使用的，可以自动设置EntityManager
 * 和Transaction为Spring Data JPA配置中指定的EntityManager和TransactionManager
 * </p>
 *
 * @author Rain
 * @version $Id: JpaEntityManagerAware.java 3807 2012-11-21 07:31:51Z wilson $
 * @since 2011-2-15
 */
public interface JpaEntityManagerAware {
    /**
     * 设置EntityManager
     *
     * @param entityManager
     */
    void setEntityManager(EntityManager entityManager);
}
