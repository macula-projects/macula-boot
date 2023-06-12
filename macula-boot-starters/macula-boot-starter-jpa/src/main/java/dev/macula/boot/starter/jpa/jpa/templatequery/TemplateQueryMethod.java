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

/**
 * TemplateQueryMethod.java 2017年11月17日
 */
package dev.macula.boot.starter.jpa.jpa.templatequery;

import dev.macula.boot.starter.jpa.jpa.TemplateQuery;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;

import java.lang.reflect.Method;

/**
 * <p>
 * <b>TemplateQueryMethod</b> TemplateQuery的JpaQueryMethod，可以获取@TemplateQuery注解
 * </p>
 *
 * @author Rain
 * @version $Id$
 * @since 2017年11月17日
 */
public class TemplateQueryMethod extends JpaQueryMethod {

    private Method method;

    /**
     * @param method
     * @param metadata
     * @param factory
     * @param extractor
     */
    public TemplateQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
        QueryExtractor extractor) {
        super(method, metadata, factory, extractor);
        this.method = method;
    }

    /**
     * 获取是否TemplateQuery的判断
     *
     * @return boolean
     */
    public boolean isTemplateQuery() {
        return null != AnnotationUtils.findAnnotation(method, TemplateQuery.class);
    }

    /**
     * 获取原始的Method
     *
     * @return Method
     */
    public Method getMethod() {
        return this.method;
    }
}
