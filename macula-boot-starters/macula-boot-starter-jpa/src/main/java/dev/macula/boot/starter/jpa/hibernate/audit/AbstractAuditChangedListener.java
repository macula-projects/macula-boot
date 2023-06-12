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
package dev.macula.boot.starter.jpa.hibernate.audit;

import org.springframework.context.ApplicationListener;

/**
 * <p>
 * <b>AbstractAuditChangedListener</b> 是数据变化事件的监听器
 * </p>
 *
 * @author Rain
 * @version $Id: AbstractAuditChangedListener.java 3807 2012-11-21 07:31:51Z wilson $
 * @since 2011-3-5
 */
public abstract class AbstractAuditChangedListener implements ApplicationListener<AuditChangedEvent> {

    @Override
    public void onApplicationEvent(AuditChangedEvent auditChangedEvent) {
        onAuditChanged(auditChangedEvent.getSource());
    }

    /**
     * 实现该方法，获取所需的数据变化实体，以便做进一步操作
     *
     * @param auditChanged 变化对象
     */
    protected abstract void onAuditChanged(AuditChanged auditChanged);

}
