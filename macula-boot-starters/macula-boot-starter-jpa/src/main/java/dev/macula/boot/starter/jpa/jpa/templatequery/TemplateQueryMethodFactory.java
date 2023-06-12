/*
 * Copyright 2004-2020 the original author or authors.
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

package dev.macula.boot.starter.jpa.jpa.templatequery;

import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * <p>
 * <b>TemplateQueryMethodFactory</b> TemplateQueryMethod创建工厂
 * </p>
 *
 * @author Rain
 * @since 2020-09-13
 */
public class TemplateQueryMethodFactory implements JpaQueryMethodFactory {

    private final QueryExtractor extractor;

    public TemplateQueryMethodFactory(QueryExtractor extractor) {

        Assert.notNull(extractor, "QueryExtractor must not be null");

        this.extractor = extractor;
    }

    @Override
    public JpaQueryMethod build(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        return new TemplateQueryMethod(method, metadata, factory, extractor);
    }
}
